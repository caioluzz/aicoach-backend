package com.aicoach.backend.service;

import com.aicoach.backend.comparison.ActivityComparisonEngine;
import com.aicoach.backend.comparison.ComparisonTolerancePolicy;
import com.aicoach.backend.dto.ActivityComparisonResponse;
import com.aicoach.backend.enums.ActivityMatchType;
import com.aicoach.backend.enums.ComplianceClassification;
import com.aicoach.backend.enums.WeeklyPlanStatus;
import com.aicoach.backend.enums.WorkoutType;
import com.aicoach.backend.models.*;
import com.aicoach.backend.repository.ActivityComparisonRepo;
import com.aicoach.backend.repository.ActivityRepo;
import com.aicoach.backend.repository.PlannedActivityRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityComparisonService {
    private final ActivityRepo activityRepo;
    private final PlannedActivityRepo plannedActivityRepo;
    private final ActivityComparisonRepo comparisonRepo;
    private final Clock clock;
    private final ActivityComparisonEngine engine = new ActivityComparisonEngine(ComparisonTolerancePolicy.V1);

    @Transactional
    public ActivityComparisonResponse compareActivity(Long activityId) {
        Activity activity = activityRepo.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Atividade não encontrada: " + activityId));
        ActivityComparison comparison = comparisonRepo.findByActivityId(activityId).orElseGet(ActivityComparison::new);
        comparison.getSteps().clear();
        comparison.setAthlete(activity.getAthlete());
        comparison.setActivity(activity);
        comparison.setCalculatedAt(Instant.now(clock));
        comparison.setToleranceVersion(ComparisonTolerancePolicy.V1.version());

        PlannedActivity planned = chooseCandidate(activity);
        if (planned == null) {
            applyUnplanned(comparison, activity);
        } else {
            applyPlanned(comparison, planned, activity);
        }
        return toResponse(comparisonRepo.save(comparison));
    }

    @Transactional(readOnly = true)
    public ActivityComparisonResponse getForActivity(Long activityId) {
        return comparisonRepo.findByActivityId(activityId).map(this::toResponse)
                .orElseThrow(() -> new ActivityComparisonNotFoundException(activityId));
    }

    @Transactional(readOnly = true)
    public boolean hasComparison(Long activityId) {
        return comparisonRepo.findByActivityId(activityId).isPresent();
    }

    @Transactional(readOnly = true)
    public List<ActivityComparisonResponse> listForAthlete(Long athleteId) {
        return comparisonRepo.findByAthleteIdOrderByCalculatedAtDesc(athleteId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public List<ActivityComparisonResponse> reconcileMissed(Long athleteId, LocalDate throughDate) {
        LocalDate cutoff = throughDate == null ? LocalDate.now(clock).minusDays(1) : throughDate;
        List<PlannedActivity> planned = plannedActivityRepo.findComparisonCandidates(
                athleteId, LocalDate.of(1970, 1, 1), cutoff).stream()
                .filter(this::isEligiblePlan).toList();
        for (PlannedActivity item : planned) {
            if (!comparisonRepo.existsByPlannedActivityId(item.getId())) {
                ActivityComparison comparison = new ActivityComparison();
                comparison.setAthlete(item.getTrainingCycle().getGlobalPlan().getAthlete());
                comparison.setPlannedActivity(item);
                comparison.setMatchType(ActivityMatchType.MISSED);
                comparison.setClassification(ComplianceClassification.NOT_EXECUTED);
                comparison.setCompliancePercentage(decimal(0));
                comparison.setToleranceVersion(ComparisonTolerancePolicy.V1.version());
                comparison.setPlannedDurationSeconds(item.getPlannedDurationSeconds());
                comparison.setPlannedDistanceMeters(item.getPlannedDistanceMeters());
                comparison.setExplanation("política=" + ComparisonTolerancePolicy.V1.version()
                        + "; atividade planejada sem corrida correspondente até " + cutoff
                        + "; percentual=0.00; classificação=NOT_EXECUTED");
                comparison.setCalculatedAt(Instant.now(clock));
                int sequence = 1;
                for (ExpandedStep expanded : expand(item)) {
                    ActivityStepComparison step = emptyStep(comparison, expanded, sequence++);
                    comparison.getSteps().add(step);
                }
                comparisonRepo.save(comparison);
            }
        }
        return comparisonRepo.findByAthleteIdOrderByCalculatedAtDesc(athleteId).stream()
                .map(this::toResponse).toList();
    }

    private PlannedActivity chooseCandidate(Activity activity) {
        LocalDate date = activity.getStartedAt().toLocalDate();
        return plannedActivityRepo.findComparisonCandidates(activity.getAthlete().getId(), date.minusDays(1), date.plusDays(1))
                .stream()
                .filter(this::isEligiblePlan)
                .filter(candidate -> comparisonRepo.findByPlannedActivityId(candidate.getId())
                        .map(existing -> existing.getActivity() != null && existing.getActivity().getId().equals(activity.getId()))
                        .orElse(true))
                .filter(candidate -> acceptable(candidate, activity, date))
                .min(Comparator.<PlannedActivity>comparingDouble(candidate -> candidateCost(candidate, activity, date))
                        .thenComparing(PlannedActivity::getId))
                .orElse(null);
    }

    private boolean isEligiblePlan(PlannedActivity planned) {
        if (planned.getWorkoutType() == WorkoutType.REST) return false;
        if (planned.getWeeklyPlan() == null) return true;
        return planned.getWeeklyPlan().getStatus() == WeeklyPlanStatus.APPROVED
                || planned.getWeeklyPlan().getStatus() == WeeklyPlanStatus.DELIVERED;
    }

    private boolean acceptable(PlannedActivity planned, Activity actual, LocalDate actualDate) {
        if (planned.getScheduledDate().equals(actualDate)) return true;
        double distanceDelta = relativeDelta(actual.getDistanceMeters(), planned.getPlannedDistanceMeters());
        double durationDelta = relativeDelta(actual.getDurationSeconds(), planned.getPlannedDurationSeconds());
        return Math.min(distanceDelta, durationDelta) <= 0.35;
    }

    private double candidateCost(PlannedActivity planned, Activity actual, LocalDate actualDate) {
        long days = Math.abs(ChronoUnit.DAYS.between(planned.getScheduledDate(), actualDate));
        return days * 1000 + relativeDelta(actual.getDistanceMeters(), planned.getPlannedDistanceMeters()) * 100
                + relativeDelta(actual.getDurationSeconds(), planned.getPlannedDurationSeconds()) * 100;
    }

    private double relativeDelta(Number actual, Number planned) {
        if (actual == null || planned == null || planned.doubleValue() <= 0) return 1.0;
        return Math.abs(actual.doubleValue() - planned.doubleValue()) / planned.doubleValue();
    }

    private void applyUnplanned(ActivityComparison comparison, Activity activity) {
        comparison.setPlannedActivity(null);
        comparison.setMatchType(ActivityMatchType.UNPLANNED);
        comparison.setClassification(ComplianceClassification.DIFFERENT);
        comparison.setCompliancePercentage(decimal(0));
        comparison.setPlannedDurationSeconds(null);
        comparison.setPlannedDistanceMeters(null);
        applyActualMetrics(comparison, activity);
        comparison.setExplanation("política=" + ComparisonTolerancePolicy.V1.version()
                + "; corrida não planejada; não existe alvo para pontuar; percentual=0.00; classificação=DIFFERENT");
    }

    private void applyPlanned(ActivityComparison comparison, PlannedActivity planned, Activity activity) {
        List<ExpandedStep> expanded = expand(planned);
        ActivityComparisonEngine.Plan plan = new ActivityComparisonEngine.Plan(
                planned.getPlannedDurationSeconds(), planned.getPlannedDistanceMeters(), expanded.stream()
                .map(item -> new ActivityComparisonEngine.PlannedStep(item.step().getId(), item.occurrence(),
                        item.step().getDurationType(), item.step().getDurationValue(),
                        item.step().getTargetPaceFastestSecondsPerKm(), item.step().getTargetPaceSlowestSecondsPerKm()))
                .toList());
        ActivityComparisonEngine.Result result = engine.compare(plan, actual(activity));
        comparison.setPlannedActivity(planned);
        comparison.setMatchType(ActivityMatchType.PLANNED);
        comparison.setClassification(result.classification());
        comparison.setCompliancePercentage(decimal(result.percentage()));
        comparison.setPlannedDurationSeconds(planned.getPlannedDurationSeconds());
        comparison.setPlannedDistanceMeters(planned.getPlannedDistanceMeters());
        applyActualMetrics(comparison, activity);
        comparison.setExplanation(result.explanation()
                + "; frequência cardíaca e cadência são reportadas, mas não pontuadas sem alvos prescritos");
        int sequence = 1;
        for (ActivityComparisonEngine.StepResult resultStep : result.steps()) {
            ExpandedStep expandedStep = expanded.get(sequence - 1);
            ActivityStepComparison step = new ActivityStepComparison();
            step.setComparison(comparison);
            step.setWorkoutStep(expandedStep.step());
            step.setSequenceNumber(sequence++);
            step.setOccurrenceIndex(expandedStep.occurrence());
            step.setAlignmentSource(resultStep.source());
            step.setClassification(resultStep.classification());
            step.setCompliancePercentage(decimal(resultStep.percentage()));
            step.setActualDurationSeconds(decimal(resultStep.durationSeconds()));
            step.setActualDistanceMeters(decimal(resultStep.distanceMeters()));
            step.setActualPaceSecondsPerKm(decimal(resultStep.paceSecondsPerKm()));
            step.setActualAverageHeartRate(decimal(resultStep.heartRate()));
            step.setActualAverageCadence(decimal(resultStep.cadence()));
            step.setIntervalStartSeconds(decimal(resultStep.startSeconds()));
            step.setIntervalEndSeconds(decimal(resultStep.endSeconds()));
            step.setIntervalStartMeters(decimal(resultStep.startMeters()));
            step.setIntervalEndMeters(decimal(resultStep.endMeters()));
            step.setExplanation(resultStep.explanation());
            comparison.getSteps().add(step);
        }
    }

    private ActivityStepComparison emptyStep(ActivityComparison comparison, ExpandedStep expanded, int sequence) {
        ActivityStepComparison step = new ActivityStepComparison();
        step.setComparison(comparison);
        step.setWorkoutStep(expanded.step());
        step.setSequenceNumber(sequence);
        step.setOccurrenceIndex(expanded.occurrence());
        step.setAlignmentSource(com.aicoach.backend.enums.StepAlignmentSource.NONE);
        step.setClassification(ComplianceClassification.NOT_EXECUTED);
        step.setCompliancePercentage(decimal(0));
        step.setActualDurationSeconds(decimal(0));
        step.setActualDistanceMeters(decimal(0));
        step.setExplanation("nenhum intervalo realizado disponível");
        return step;
    }

    private List<ExpandedStep> expand(PlannedActivity planned) {
        List<ExpandedStep> result = new ArrayList<>();
        planned.getWorkoutBlocks().stream().sorted(Comparator.comparing(WorkoutBlock::getBlockOrder)).forEach(block -> {
            for (int occurrence = 1; occurrence <= block.getIterations(); occurrence++) {
                int currentOccurrence = occurrence;
                block.getSteps().stream().sorted(Comparator.comparing(WorkoutStep::getStepOrder))
                        .forEach(step -> result.add(new ExpandedStep(step, currentOccurrence)));
            }
        });
        return result;
    }

    private ActivityComparisonEngine.Actual actual(Activity activity) {
        List<ActivityComparisonEngine.RecordPoint> records = activity.getRecords().stream()
                .filter(record -> record.getElapsedS() != null && record.getDistanceKm() != null)
                .map(record -> new ActivityComparisonEngine.RecordPoint(record.getElapsedS().doubleValue(),
                        record.getDistanceKm().doubleValue() * 1000.0, number(record.getHeartRate()),
                        number(record.getCadence()))).toList();
        List<ActivityComparisonEngine.LapSegment> laps = activity.getLaps().stream()
                .sorted(Comparator.comparing(Lap::getLapNumber))
                .map(lap -> new ActivityComparisonEngine.LapSegment(number(lap.getDurationS()),
                        lap.getDistanceKm() == null ? null : lap.getDistanceKm().doubleValue() * 1000.0,
                        number(lap.getAvgHr()), number(lap.getAvgCadence()))).toList();
        return new ActivityComparisonEngine.Actual(activity.getDurationSeconds(), activity.getDistanceMeters(),
                number(activity.getAverageHeartRate()), number(activity.getAvgCadence()), records, laps);
    }

    private void applyActualMetrics(ActivityComparison comparison, Activity activity) {
        comparison.setActualDurationSeconds(decimal(activity.getDurationSeconds()));
        comparison.setActualDistanceMeters(decimal(activity.getDistanceMeters()));
        Double pace = activity.getDistanceMeters() != null && activity.getDistanceMeters() > 0
                && activity.getDurationSeconds() != null ? activity.getDurationSeconds() / activity.getDistanceMeters() * 1000 : null;
        comparison.setActualPaceSecondsPerKm(decimal(pace));
        comparison.setActualAverageHeartRate(decimal(activity.getAverageHeartRate()));
        comparison.setActualAverageCadence(decimal(activity.getAvgCadence()));
    }

    private ActivityComparisonResponse toResponse(ActivityComparison comparison) {
        ActivityComparisonResponse.Metrics metrics = new ActivityComparisonResponse.Metrics(
                comparison.getPlannedDurationSeconds(), comparison.getActualDurationSeconds(),
                comparison.getPlannedDistanceMeters(), comparison.getActualDistanceMeters(),
                plannedPace(comparison),
                comparison.getActualPaceSecondsPerKm(), comparison.getActualAverageHeartRate(),
                comparison.getActualAverageCadence());
        List<ActivityComparisonResponse.Step> steps = comparison.getSteps().stream().map(step ->
                new ActivityComparisonResponse.Step(step.getSequenceNumber(), step.getWorkoutStep().getId(),
                        step.getOccurrenceIndex(), step.getWorkoutStep().getDurationType(),
                        step.getWorkoutStep().getDurationValue(),
                        step.getWorkoutStep().getTargetPaceFastestSecondsPerKm(),
                        step.getWorkoutStep().getTargetPaceSlowestSecondsPerKm(),
                        step.getAlignmentSource(), step.getClassification(),
                        step.getCompliancePercentage(), step.getActualDurationSeconds(), step.getActualDistanceMeters(),
                        step.getActualPaceSecondsPerKm(), step.getActualAverageHeartRate(),
                        step.getActualAverageCadence(), step.getIntervalStartSeconds(), step.getIntervalEndSeconds(),
                        step.getIntervalStartMeters(), step.getIntervalEndMeters(), step.getExplanation())).toList();
        return new ActivityComparisonResponse(comparison.getId(), comparison.getAthlete().getId(),
                comparison.getActivity() == null ? null : comparison.getActivity().getId(),
                comparison.getPlannedActivity() == null ? null : comparison.getPlannedActivity().getId(),
                comparison.getMatchType(), comparison.getClassification(), comparison.getCompliancePercentage(),
                comparison.getToleranceVersion(), metrics, comparison.getExplanation(), comparison.getCalculatedAt(), steps);
    }

    private BigDecimal decimal(Number value) {
        return value == null ? null : BigDecimal.valueOf(value.doubleValue()).setScale(2, RoundingMode.HALF_UP);
    }
    private BigDecimal plannedPace(ActivityComparison comparison) {
        Integer duration = comparison.getPlannedDurationSeconds();
        Integer distance = comparison.getPlannedDistanceMeters();
        return duration == null || distance == null || distance <= 0
                ? null : decimal(duration.doubleValue() / distance * 1000);
    }
    private Double number(Number value) { return value == null ? null : value.doubleValue(); }
    private record ExpandedStep(WorkoutStep step, int occurrence) {}
}
