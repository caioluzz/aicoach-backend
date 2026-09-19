package com.aicoach.backend.service;

import com.aicoach.backend.client.GarminWorkoutGateway;
import com.aicoach.backend.dto.GarminDeliveryResponse;
import com.aicoach.backend.dto.GarminPreviewResponse;
import com.aicoach.backend.dto.GarminWorkoutContract;
import com.aicoach.backend.enums.GarminDeliveryStatus;
import com.aicoach.backend.enums.WeeklyPlanStatus;
import com.aicoach.backend.models.*;
import com.aicoach.backend.repository.GarminWorkoutDeliveryRepo;
import com.aicoach.backend.repository.WeeklyPlanRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GarminWorkoutDeliveryService {
    private final WeeklyPlanRepo weeklyPlanRepo;
    private final GarminWorkoutDeliveryRepo deliveryRepo;
    private final GarminWorkoutGateway gateway;
    private final Clock clock;

    @Transactional(readOnly = true)
    public GarminPreviewResponse preview(Long athleteId, Long weeklyPlanId) {
        WeeklyPlan plan = findPlan(athleteId, weeklyPlanId);
        ensureApproved(plan);
        List<GarminPreviewResponse.Item> sessions = orderedSessions(plan).stream().map(session -> {
            GarminWorkoutGateway.Preview preview = gateway.preview(toContract(session));
            return new GarminPreviewResponse.Item(session.getId(), session.getSessionOrder(), session.getName(),
                    session.getScheduledDate(), preview.hash(), preview.payload());
        }).toList();
        return new GarminPreviewResponse(plan.getId(), plan.getVersion(), sessions);
    }

    @Transactional(noRollbackFor = GarminDeliveryException.class)
    public GarminDeliveryResponse deliver(Long athleteId, Long weeklyPlanId) {
        WeeklyPlan plan = findPlan(athleteId, weeklyPlanId);
        ensureApproved(plan);
        GarminWorkoutGateway.Credentials credentials = credentials(plan.getAthlete());
        List<SessionPreview> previews;
        try {
            previews = orderedSessions(plan).stream().map(session -> {
                GarminWorkoutContract contract = toContract(session);
                return new SessionPreview(session, contract, gateway.preview(contract));
            }).toList();
        } catch (RuntimeException exception) {
            throw new GarminDeliveryException("A pré-visualização Garmin falhou antes do envio");
        }
        for (SessionPreview sessionPreview : previews) {
            PlannedActivity session = sessionPreview.session();
            GarminWorkoutContract contract = sessionPreview.contract();
            GarminWorkoutGateway.Preview preview = sessionPreview.preview();
            String key = idempotencyKey(athleteId, session.getId(), plan.getVersion(), preview.hash());
            GarminWorkoutDelivery delivery = deliveryRepo
                    .findByAthleteIdAndPlannedActivityIdAndPlanVersionAndContentHash(
                            athleteId, session.getId(), plan.getVersion(), preview.hash())
                    .orElseGet(() -> newDelivery(plan, session, preview.hash(), key));
            if (delivery.getStatus() == GarminDeliveryStatus.SCHEDULED
                    || delivery.getStatus() == GarminDeliveryStatus.CONFIRMED) {
                continue;
            }
            delivery.setStatus(GarminDeliveryStatus.DELIVERING);
            delivery.setAttemptCount(delivery.getAttemptCount() + 1);
            delivery.setLastError(null);
            delivery.setUpdatedAt(Instant.now(clock));
            deliveryRepo.saveAndFlush(delivery);
            try {
                GarminWorkoutGateway.Delivery result = gateway.deliver(credentials, key, contract);
                if (result == null || result.workoutId() == null) {
                    throw new IllegalStateException("empty Garmin delivery response");
                }
                delivery.setExternalWorkoutId(result.workoutId());
                delivery.setExternalScheduleId(result.scheduledWorkoutId());
                delivery.setStatus(GarminDeliveryStatus.SCHEDULED);
                delivery.setScheduledAt(Instant.now(clock));
                delivery.setUpdatedAt(Instant.now(clock));
            } catch (RuntimeException exception) {
                fail(delivery);
                throw new GarminDeliveryException("A entrega Garmin falhou; o estado foi preservado para nova tentativa");
            }
        }
        return response(plan, athleteId);
    }

    @Transactional(noRollbackFor = GarminDeliveryException.class)
    public GarminDeliveryResponse update(Long athleteId, Long weeklyPlanId, Long deliveryId) {
        WeeklyPlan plan = findPlan(athleteId, weeklyPlanId);
        ensureManageable(plan);
        GarminWorkoutDelivery delivery = findDelivery(athleteId, weeklyPlanId, deliveryId);
        if (delivery.getExternalWorkoutId() == null
                || delivery.getStatus() == GarminDeliveryStatus.CANCELLED) {
            throw new WeeklyPlanStateException("Somente uma entrega Garmin ativa pode ser atualizada");
        }
        delivery.setStatus(GarminDeliveryStatus.DELIVERING);
        delivery.setAttemptCount(delivery.getAttemptCount() + 1);
        delivery.setLastError(null);
        delivery.setUpdatedAt(Instant.now(clock));
        deliveryRepo.flush();
        try {
            GarminWorkoutGateway.Delivery result = gateway.update(credentials(plan.getAthlete()),
                    delivery.getExternalWorkoutId(), delivery.getExternalScheduleId(),
                    delivery.getIdempotencyKey(), toContract(delivery.getPlannedActivity()));
            delivery.setExternalScheduleId(result.scheduledWorkoutId());
            delivery.setScheduledDate(delivery.getPlannedActivity().getScheduledDate());
            delivery.setStatus(GarminDeliveryStatus.SCHEDULED);
            delivery.setScheduledAt(Instant.now(clock));
            delivery.setConfirmedAt(null);
            delivery.setUpdatedAt(Instant.now(clock));
            if (plan.getStatus() == WeeklyPlanStatus.DELIVERED) plan.setStatus(WeeklyPlanStatus.APPROVED);
        } catch (RuntimeException exception) {
            fail(delivery);
            throw new GarminDeliveryException("A atualização Garmin falhou; a entrega pode ser tentada novamente");
        }
        return response(plan, athleteId);
    }

    @Transactional(noRollbackFor = GarminDeliveryException.class)
    public GarminDeliveryResponse confirm(Long athleteId, Long weeklyPlanId) {
        WeeklyPlan plan = findPlan(athleteId, weeklyPlanId);
        ensureManageable(plan);
        GarminWorkoutGateway.Credentials credentials = credentials(plan.getAthlete());
        List<GarminWorkoutDelivery> deliveries = deliveryRepo
                .findByAthleteIdAndWeeklyPlanIdOrderByPlannedActivitySessionOrder(athleteId, weeklyPlanId);
        if (deliveries.isEmpty()) {
            throw new WeeklyPlanStateException("O plano ainda não possui entregas Garmin");
        }
        if (deliveries.size() != plan.getSessions().size()) {
            throw new WeeklyPlanStateException("Todas as sessões precisam ser enviadas antes da confirmação");
        }
        for (GarminWorkoutDelivery delivery : deliveries) {
            if (delivery.getStatus() == GarminDeliveryStatus.CONFIRMED) continue;
            if (delivery.getStatus() != GarminDeliveryStatus.SCHEDULED
                    || delivery.getExternalWorkoutId() == null) {
                throw new WeeklyPlanStateException("Todas as sessões precisam estar agendadas antes da confirmação");
            }
            try {
                GarminWorkoutGateway.Confirmation confirmation = gateway.confirm(credentials,
                        delivery.getExternalWorkoutId(), delivery.getExternalScheduleId(),
                        delivery.getScheduledDate(), delivery.getIdempotencyKey());
                if (!Boolean.TRUE.equals(confirmation.confirmed())) {
                    throw new GarminDeliveryException("A sessão ainda não foi encontrada no calendário Garmin");
                }
                delivery.setExternalScheduleId(confirmation.scheduledWorkoutId());
                delivery.setStatus(GarminDeliveryStatus.CONFIRMED);
                delivery.setConfirmedAt(Instant.now(clock));
                delivery.setUpdatedAt(Instant.now(clock));
            } catch (GarminDeliveryException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                fail(delivery);
                throw new GarminDeliveryException("A confirmação do calendário Garmin falhou");
            }
        }
        plan.setStatus(WeeklyPlanStatus.DELIVERED);
        return response(plan, athleteId);
    }

    @Transactional(noRollbackFor = GarminDeliveryException.class)
    public GarminDeliveryResponse cancel(Long athleteId, Long weeklyPlanId, Long deliveryId) {
        WeeklyPlan plan = findPlan(athleteId, weeklyPlanId);
        ensureManageable(plan);
        GarminWorkoutDelivery delivery = findDelivery(athleteId, weeklyPlanId, deliveryId);
        if (delivery.getStatus() == GarminDeliveryStatus.CANCELLED) return response(plan, athleteId);
        if (delivery.getExternalWorkoutId() == null) {
            throw new WeeklyPlanStateException("A entrega não possui identificador externo para cancelamento");
        }
        delivery.setStatus(GarminDeliveryStatus.CANCELLING);
        delivery.setAttemptCount(delivery.getAttemptCount() + 1);
        delivery.setUpdatedAt(Instant.now(clock));
        deliveryRepo.flush();
        try {
            gateway.cancel(credentials(plan.getAthlete()), delivery.getExternalWorkoutId(),
                    delivery.getExternalScheduleId());
            delivery.setStatus(GarminDeliveryStatus.CANCELLED);
            delivery.setCancelledAt(Instant.now(clock));
            delivery.setConfirmedAt(null);
            delivery.setUpdatedAt(Instant.now(clock));
            plan.setStatus(WeeklyPlanStatus.APPROVED);
        } catch (RuntimeException exception) {
            fail(delivery);
            throw new GarminDeliveryException("O cancelamento Garmin falhou; a entrega pode ser tentada novamente");
        }
        return response(plan, athleteId);
    }

    private GarminWorkoutDelivery newDelivery(WeeklyPlan plan, PlannedActivity session,
                                              String contentHash, String idempotencyKey) {
        Instant now = Instant.now(clock);
        GarminWorkoutDelivery delivery = new GarminWorkoutDelivery();
        delivery.setAthlete(plan.getAthlete());
        delivery.setWeeklyPlan(plan);
        delivery.setPlannedActivity(session);
        delivery.setPlanVersion(plan.getVersion());
        delivery.setContentHash(contentHash);
        delivery.setIdempotencyKey(idempotencyKey);
        delivery.setStatus(GarminDeliveryStatus.PENDING);
        delivery.setScheduledDate(session.getScheduledDate());
        delivery.setCreatedAt(now);
        delivery.setUpdatedAt(now);
        return delivery;
    }

    private void fail(GarminWorkoutDelivery delivery) {
        delivery.setStatus(GarminDeliveryStatus.FAILED);
        delivery.setLastError("Falha temporária na integração Garmin");
        delivery.setUpdatedAt(Instant.now(clock));
    }

    private GarminWorkoutContract toContract(PlannedActivity session) {
        return new GarminWorkoutContract(GarminWorkoutContract.SCHEMA_VERSION, session.getName(),
                session.getScheduledDate(), session.getWorkoutBlocks().stream()
                .sorted(Comparator.comparing(WorkoutBlock::getBlockOrder))
                .map(block -> new GarminWorkoutContract.Block(block.getBlockOrder(), block.getIterations(),
                        block.getSteps().stream().sorted(Comparator.comparing(WorkoutStep::getStepOrder))
                                .map(step -> new GarminWorkoutContract.Step(step.getStepOrder(), step.getStepType(),
                                        step.getDurationType(), step.getDurationValue(), step.getTargetZone(),
                                        step.getTargetPaceFastestSecondsPerKm(),
                                        step.getTargetPaceSlowestSecondsPerKm(), step.getInstruction()))
                                .toList())).toList());
    }

    private GarminWorkoutGateway.Credentials credentials(Athlete athlete) {
        if (athlete.getGarminEmail() == null || athlete.getGarminEmail().isBlank()
                || athlete.getGarminPassword() == null || athlete.getGarminPassword().isBlank()) {
            throw new WeeklyPlanPrerequisiteException("O atleta precisa configurar credenciais Garmin");
        }
        return new GarminWorkoutGateway.Credentials(athlete.getGarminEmail(), athlete.getGarminPassword());
    }

    private WeeklyPlan findPlan(Long athleteId, Long weeklyPlanId) {
        return weeklyPlanRepo.findByIdAndAthleteId(weeklyPlanId, athleteId)
                .orElseThrow(() -> new WeeklyPlanNotFoundException(weeklyPlanId));
    }

    private GarminWorkoutDelivery findDelivery(Long athleteId, Long weeklyPlanId, Long deliveryId) {
        return deliveryRepo.findByIdAndAthleteIdAndWeeklyPlanId(deliveryId, athleteId, weeklyPlanId)
                .orElseThrow(() -> new GarminDeliveryNotFoundException(deliveryId));
    }

    private void ensureApproved(WeeklyPlan plan) {
        if (plan.getStatus() != WeeklyPlanStatus.APPROVED) {
            throw new WeeklyPlanStateException("Somente um plano semanal APPROVED pode ser entregue ao Garmin");
        }
    }

    private void ensureManageable(WeeklyPlan plan) {
        if (plan.getStatus() != WeeklyPlanStatus.APPROVED
                && plan.getStatus() != WeeklyPlanStatus.DELIVERED) {
            throw new WeeklyPlanStateException("Somente entregas de planos APPROVED ou DELIVERED podem ser gerenciadas");
        }
    }

    private List<PlannedActivity> orderedSessions(WeeklyPlan plan) {
        return plan.getSessions().stream().sorted(Comparator.comparing(PlannedActivity::getSessionOrder)).toList();
    }

    private GarminDeliveryResponse response(WeeklyPlan plan, Long athleteId) {
        List<GarminDeliveryResponse.Item> items = deliveryRepo
                .findByAthleteIdAndWeeklyPlanIdOrderByPlannedActivitySessionOrder(athleteId, plan.getId())
                .stream().map(delivery -> new GarminDeliveryResponse.Item(delivery.getId(),
                        delivery.getPlannedActivity().getId(), delivery.getPlannedActivity().getSessionOrder(),
                        delivery.getPlannedActivity().getName(), delivery.getStatus(), delivery.getContentHash(),
                        delivery.getScheduledDate(), delivery.getExternalWorkoutId(),
                        delivery.getExternalScheduleId(), delivery.getAttemptCount(), delivery.getLastError(),
                        delivery.getUpdatedAt(), delivery.getConfirmedAt(), delivery.getCancelledAt()))
                .toList();
        return new GarminDeliveryResponse(plan.getId(), plan.getVersion(), items);
    }

    private String idempotencyKey(Long athleteId, Long activityId, Integer version, String contentHash) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String source = athleteId + ":" + activityId + ":" + version + ":" + contentHash;
            return HexFormat.of().formatHex(digest.digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private record SessionPreview(PlannedActivity session, GarminWorkoutContract contract,
                                  GarminWorkoutGateway.Preview preview) {}
}
