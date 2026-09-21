package com.aicoach.backend.service;

import com.aicoach.backend.dto.SecondaryRaceRequest;
import com.aicoach.backend.dto.SecondaryRaceResponse;
import com.aicoach.backend.enums.*;
import com.aicoach.backend.models.*;
import com.aicoach.backend.repository.*;
import com.aicoach.backend.secondaryrace.SecondaryRaceAdjustmentEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SecondaryRaceService {
    private static final int PRIMARY_RACE_PROTECTION_DAYS = 7;

    private final AthleteRepo athleteRepo;
    private final GlobalPlanRepo globalPlanRepo;
    private final SecondaryRaceRepo secondaryRaceRepo;
    private final PlannedActivityRepo plannedActivityRepo;
    private final SecondaryRaceAdjustmentEngine adjustmentEngine;
    private final Clock clock;

    @Transactional
    public SecondaryRaceResponse create(Long athleteId, SecondaryRaceRequest request) {
        Athlete athlete = athleteRepo.findById(athleteId)
                .orElseThrow(() -> new AthleteNotFoundException(athleteId));
        GlobalPlan plan = globalPlanRepo.findByIdAndAthleteId(request.globalPlanId(), athleteId)
                .orElseThrow(() -> new SecondaryRaceValidationException(
                        "O plano geral informado não pertence ao atleta"));
        validate(plan, request);

        List<PlannedActivity> sessions = plannedActivityRepo.findSecondaryRaceWindow(
                athleteId, plan.getId(), request.raceDate().minusDays(5), request.raceDate().plusDays(4),
                EnumSet.of(WeeklyPlanStatus.VALIDATED, WeeklyPlanStatus.APPROVED,
                        WeeklyPlanStatus.DELIVERED));
        Map<Long, PlannedActivity> sessionsById = sessions.stream()
                .collect(Collectors.toMap(PlannedActivity::getId, Function.identity()));
        SecondaryRaceAdjustmentEngine.Decision decision = adjustmentEngine.evaluate(
                new SecondaryRaceAdjustmentEngine.Input(request.raceDate(), request.distanceMeters(),
                        request.targetTimeSeconds(), request.priority(), sessions.stream()
                        .map(item -> new SecondaryRaceAdjustmentEngine.Session(item.getId(),
                                item.getWeeklyPlan().getId(), item.getScheduledDate(), item.getWorkoutType(),
                                item.getPlannedDistanceMeters(), item.getPlannedDurationSeconds())).toList()));

        SecondaryRace race = new SecondaryRace();
        race.setAthlete(athlete);
        race.setGlobalPlan(plan);
        race.setPrimaryObjective(plan.getObjective());
        race.setTitle(request.title().trim());
        race.setRaceDate(request.raceDate());
        race.setDistanceMeters(request.distanceMeters());
        race.setTargetTimeSeconds(request.targetTimeSeconds());
        race.setPriority(request.priority());
        race.setStatus(SecondaryRaceStatus.SCHEDULED);
        race.setRuleVersion(SecondaryRaceAdjustmentEngine.VERSION);
        race.setWindowStart(decision.windowStart());
        race.setWindowEnd(decision.windowEnd());
        race.setRationale(decision.rationale() + " A prova-alvo principal permanece "
                + plan.getObjective().getTitle() + " em " + plan.getObjective().getTargetDate() + ".");
        race.setCreatedAt(clock.instant());
        race.setAdjustments(decision.adjustments().stream()
                .map(item -> adjustment(race, item, sessionsById)).toList());
        return toResponse(secondaryRaceRepo.save(race));
    }

    @Transactional(readOnly = true)
    public List<SecondaryRaceResponse> list(Long athleteId) {
        if (!athleteRepo.existsById(athleteId)) throw new AthleteNotFoundException(athleteId);
        return secondaryRaceRepo.findByAthleteIdOrderByRaceDate(athleteId).stream()
                .map(this::toResponse).toList();
    }

    private void validate(GlobalPlan plan, SecondaryRaceRequest request) {
        if (plan.getStatus() != SeasonPlanStatus.APPROVED) {
            throw new SecondaryRaceValidationException(
                    "Provas intermediárias exigem um plano geral aprovado");
        }
        if (request.priority() == RacePriority.A_RACE) {
            throw new SecondaryRaceValidationException(
                    "A prova intermediária deve ter prioridade B_RACE ou C_RACE");
        }
        LocalDate today = LocalDate.now(clock);
        if (request.raceDate().isBefore(today)) {
            throw new SecondaryRaceValidationException("A prova intermediária não pode estar no passado");
        }
        if (request.raceDate().isBefore(plan.getStartDate())
                || request.raceDate().isAfter(plan.getEndDate())) {
            throw new SecondaryRaceValidationException(
                    "A prova intermediária deve estar dentro do período do plano geral");
        }
        LocalDate primaryDate = plan.getObjective().getTargetDate();
        if (!request.raceDate().plusDays(PRIMARY_RACE_PROTECTION_DAYS).isBefore(primaryDate)) {
            throw new SecondaryRaceValidationException(
                    "Mantenha ao menos oito dias entre a prova intermediária e a prova-alvo principal");
        }
        if (secondaryRaceRepo.existsByGlobalPlanIdAndRaceDate(plan.getId(), request.raceDate())) {
            throw new SecondaryRaceValidationException(
                    "Já existe uma prova intermediária nessa data para o plano");
        }
    }

    private SecondaryRaceAdjustment adjustment(SecondaryRace race,
                                                SecondaryRaceAdjustmentEngine.Adjustment value,
                                                Map<Long, PlannedActivity> sessionsById) {
        SecondaryRaceAdjustment item = new SecondaryRaceAdjustment();
        item.setSecondaryRace(race);
        PlannedActivity planned = value.sessionId() == null ? null : sessionsById.get(value.sessionId());
        item.setPlannedActivity(planned);
        item.setWeeklyPlan(planned == null ? null : planned.getWeeklyPlan());
        item.setAdjustmentWindow(value.window());
        item.setAction(value.action());
        item.setOriginalDate(value.originalDate());
        item.setProposedDate(value.proposedDate());
        item.setOriginalWorkoutType(value.originalType());
        item.setProposedWorkoutType(value.proposedType());
        item.setOriginalDistanceMeters(value.originalDistanceMeters());
        item.setProposedDistanceMeters(value.proposedDistanceMeters());
        item.setOriginalDurationSeconds(value.originalDurationSeconds());
        item.setProposedDurationSeconds(value.proposedDurationSeconds());
        item.setLoadPercent(value.loadPercent());
        item.setRationale(value.rationale());
        return item;
    }

    private SecondaryRaceResponse toResponse(SecondaryRace race) {
        Objective primary = race.getPrimaryObjective();
        return new SecondaryRaceResponse(race.getId(), race.getAthlete().getId(),
                race.getGlobalPlan().getId(), new SecondaryRaceResponse.PrimaryTarget(primary.getId(),
                primary.getTitle(), primary.getTargetDate(), primary.getTargetDistance_m(),
                primary.getPriority()), race.getTitle(), race.getRaceDate(), race.getDistanceMeters(),
                race.getTargetTimeSeconds(), race.getPriority(), race.getStatus(), race.getRuleVersion(),
                race.getWindowStart(), race.getWindowEnd(), race.getRationale(), race.getCreatedAt(),
                race.getAdjustments().stream().map(item -> new SecondaryRaceResponse.Adjustment(
                        item.getWeeklyPlan() == null ? null : item.getWeeklyPlan().getId(),
                        item.getPlannedActivity() == null ? null : item.getPlannedActivity().getId(),
                        item.getAdjustmentWindow(), item.getAction(), item.getOriginalDate(),
                        item.getProposedDate(), item.getOriginalWorkoutType(), item.getProposedWorkoutType(),
                        item.getOriginalDistanceMeters(), item.getProposedDistanceMeters(),
                        item.getOriginalDurationSeconds(), item.getProposedDurationSeconds(),
                        item.getLoadPercent(), item.getRationale())).toList());
    }
}
