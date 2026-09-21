package com.aicoach.backend.service;

import com.aicoach.backend.adaptation.AdaptationRuleEngine;
import com.aicoach.backend.dto.AdaptationDecisionResponse;
import com.aicoach.backend.dto.FeedbackRequest;
import com.aicoach.backend.enums.*;
import com.aicoach.backend.models.*;
import com.aicoach.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackAdaptationService {
    private static final int REVIEW_WINDOW_DAYS = 7;
    private static final int MATERIALITY_WINDOW_DAYS = 14;

    private final AthleteRepo athleteRepo;
    private final ActivityRepo activityRepo;
    private final ActivityComparisonRepo comparisonRepo;
    private final AthleteFeedbackRepo feedbackRepo;
    private final AdaptationDecisionRepo decisionRepo;
    private final PlannedActivityRepo plannedActivityRepo;
    private final AdaptationRuleEngine ruleEngine;
    private final Clock clock;

    @Transactional
    public AdaptationDecisionResponse submit(Long athleteId, FeedbackRequest request) {
        Athlete athlete = athleteRepo.findById(athleteId)
                .orElseThrow(() -> new AthleteNotFoundException(athleteId));
        Activity activity = resolveActivity(athleteId, request.activityId());
        validate(request, activity);

        AthleteFeedback feedback = new AthleteFeedback();
        feedback.setAthlete(athlete);
        feedback.setActivity(activity);
        feedback.setFeedbackDate(request.feedbackDate());
        feedback.setPerceivedEffort(request.perceivedEffort());
        feedback.setFatigue(request.fatigue());
        feedback.setSleepHours(request.sleepHours());
        feedback.setPainSeverity(request.painSeverity());
        feedback.setPainLocation(normalize(request.painLocation()));
        feedback.setFeeling(request.feeling());
        feedback.setComment(normalize(request.comment()));
        feedback.setCreatedAt(clock.instant());
        feedbackRepo.save(feedback);

        ActivityComparison comparison = activity == null ? null
                : comparisonRepo.findByActivityId(activity.getId()).orElse(null);
        long recentReductions = decisionRepo.countByAthleteIdAndAlertLevelInAndCreatedAtGreaterThanEqual(
                athleteId, EnumSet.of(AdaptationAlertLevel.REDUCE_LOAD,
                        AdaptationAlertLevel.STOP_AND_ASSESS),
                clock.instant().minus(MATERIALITY_WINDOW_DAYS, ChronoUnit.DAYS));
        AdaptationRuleEngine.Decision evaluated = ruleEngine.evaluate(new AdaptationRuleEngine.Input(
                request.perceivedEffort(), request.fatigue(), request.sleepHours(), request.painSeverity(),
                comparison == null ? null : comparison.getClassification(),
                comparison == null ? null : comparison.getCompliancePercentage(), recentReductions));

        AdaptationDecision decision = new AdaptationDecision();
        decision.setAthlete(athlete);
        decision.setFeedback(feedback);
        decision.setComparison(comparison);
        decision.setRuleVersion(AdaptationRuleEngine.VERSION);
        decision.setAlertLevel(evaluated.alertLevel());
        decision.setLoadReductionPercent(evaluated.loadReductionPercent());
        decision.setAllowIntensity(evaluated.allowIntensity());
        decision.setWeeklyReviewRequired(evaluated.weeklyReviewRequired());
        decision.setSeasonPlanReviewProposed(evaluated.seasonPlanReviewProposed());
        decision.setMaterialCause(evaluated.materialCause());
        decision.setRationale(evaluated.rationale());
        decision.setEvidenceSnapshot(evidenceSnapshot(request, comparison, recentReductions));
        decision.setCreatedAt(clock.instant());
        decision.setWorkoutAdjustments(reviewUpcoming(athleteId, request.feedbackDate(), decision, evaluated));
        return toResponse(decisionRepo.save(decision));
    }

    @Transactional(readOnly = true)
    public List<AdaptationDecisionResponse> history(Long athleteId) {
        if (!athleteRepo.existsById(athleteId)) throw new AthleteNotFoundException(athleteId);
        return decisionRepo.findByAthleteIdOrderByCreatedAtDesc(athleteId).stream()
                .map(this::toResponse).toList();
    }

    private Activity resolveActivity(Long athleteId, Long activityId) {
        if (activityId == null) return null;
        return activityRepo.findByIdAndAthleteId(activityId, athleteId)
                .orElseThrow(() -> new FeedbackValidationException(
                        "A atividade informada não pertence ao atleta"));
    }

    private void validate(FeedbackRequest request, Activity activity) {
        if (request.feedbackDate().isAfter(LocalDate.now(clock))) {
            throw new FeedbackValidationException("A data do feedback não pode estar no futuro");
        }
        if (request.painSeverity() > 0 && normalize(request.painLocation()) == null) {
            throw new FeedbackValidationException("A localização da dor é obrigatória quando há dor");
        }
        if (activity != null && !activity.getStartedAt().toLocalDate().equals(request.feedbackDate())) {
            throw new FeedbackValidationException(
                    "Feedback associado a atividade deve usar a data de início da atividade");
        }
    }

    private List<WorkoutAdjustment> reviewUpcoming(Long athleteId, LocalDate date,
                                                    AdaptationDecision parent,
                                                    AdaptationRuleEngine.Decision decision) {
        return plannedActivityRepo.findUnexecutedUpcoming(athleteId, date.plusDays(1),
                        date.plusDays(REVIEW_WINDOW_DAYS),
                        EnumSet.of(WeeklyPlanStatus.APPROVED, WeeklyPlanStatus.DELIVERED)).stream()
                .map(activity -> adjustment(parent, activity, decision)).toList();
    }

    private WorkoutAdjustment adjustment(AdaptationDecision parent, PlannedActivity activity,
                                         AdaptationRuleEngine.Decision decision) {
        WorkoutAdjustment item = new WorkoutAdjustment();
        item.setDecision(parent);
        item.setPlannedActivity(activity);
        item.setScheduledDate(activity.getScheduledDate());
        WorkoutAdjustmentAction action = actionFor(activity, decision);
        item.setAction(action);
        item.setOriginalDistanceMeters(activity.getPlannedDistanceMeters());
        item.setOriginalDurationSeconds(activity.getPlannedDurationSeconds());
        item.setOriginalStressPoints(activity.getCalculatedStressPoints());
        int keepPercent = 100 - decision.loadReductionPercent();
        item.setProposedDistanceMeters(scale(activity.getPlannedDistanceMeters(), keepPercent));
        item.setProposedDurationSeconds(scale(activity.getPlannedDurationSeconds(), keepPercent));
        item.setProposedStressPoints(scale(activity.getCalculatedStressPoints(), keepPercent));
        item.setRationale(switch (action) {
            case KEEP -> "Manter: nenhuma causa material exige alteração desta sessão.";
            case REDUCE -> "Reduzir volume e duração conforme a regra determinística de recuperação.";
            case REPLACE_WITH_EASY -> "Retirar intensidade e aplicar a redução de carga indicada.";
            case REST -> "Suspender a sessão até avaliação e novo feedback.";
        });
        return item;
    }

    private WorkoutAdjustmentAction actionFor(PlannedActivity activity,
                                               AdaptationRuleEngine.Decision decision) {
        if (activity.getWorkoutType() == WorkoutType.REST) return WorkoutAdjustmentAction.KEEP;
        if (decision.loadReductionPercent() >= 100) return WorkoutAdjustmentAction.REST;
        if (!decision.allowIntensity() && (activity.getWorkoutType() == WorkoutType.QUALITY_1
                || activity.getWorkoutType() == WorkoutType.QUALITY_2
                || activity.getWorkoutType() == WorkoutType.RACE)) {
            return WorkoutAdjustmentAction.REPLACE_WITH_EASY;
        }
        if (decision.loadReductionPercent() > 0) return WorkoutAdjustmentAction.REDUCE;
        return WorkoutAdjustmentAction.KEEP;
    }

    private Integer scale(Integer value, int percent) {
        if (value == null) return null;
        return BigDecimal.valueOf(value).multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).intValue();
    }

    private String evidenceSnapshot(FeedbackRequest request, ActivityComparison comparison,
                                    long recentReductions) {
        String execution = comparison == null ? "none" : comparison.getClassification() + ":"
                + comparison.getCompliancePercentage();
        return "rpe=" + request.perceivedEffort() + ";fatigue=" + request.fatigue()
                + ";sleepHours=" + request.sleepHours() + ";pain=" + request.painSeverity()
                + ";execution=" + execution + ";recentReductions=" + recentReductions;
    }

    private AdaptationDecisionResponse toResponse(AdaptationDecision decision) {
        AthleteFeedback feedback = decision.getFeedback();
        ActivityComparison comparison = decision.getComparison();
        return new AdaptationDecisionResponse(decision.getId(), decision.getAthlete().getId(),
                new AdaptationDecisionResponse.Feedback(feedback.getId(),
                        feedback.getActivity() == null ? null : feedback.getActivity().getId(),
                        feedback.getFeedbackDate(), feedback.getPerceivedEffort(), feedback.getFatigue(),
                        feedback.getSleepHours(), feedback.getPainSeverity(), feedback.getPainLocation(),
                        feedback.getFeeling(), feedback.getComment()),
                new AdaptationDecisionResponse.Evidence(comparison == null ? null : comparison.getId(),
                        comparison == null ? null : comparison.getClassification(),
                        comparison == null ? null : comparison.getCompliancePercentage(),
                        decision.getEvidenceSnapshot()),
                decision.getRuleVersion(), decision.getAlertLevel(), decision.getLoadReductionPercent(),
                decision.isAllowIntensity(), decision.isWeeklyReviewRequired(),
                decision.isSeasonPlanReviewProposed(), decision.getMaterialCause(),
                decision.getRationale(), decision.getCreatedAt(), decision.getWorkoutAdjustments().stream()
                .map(item -> new AdaptationDecisionResponse.WorkoutChange(
                        item.getPlannedActivity().getId(), item.getScheduledDate(), item.getAction(),
                        item.getOriginalDistanceMeters(), item.getProposedDistanceMeters(),
                        item.getOriginalDurationSeconds(), item.getProposedDurationSeconds(),
                        item.getOriginalStressPoints(), item.getProposedStressPoints(), item.getRationale()))
                .toList());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
