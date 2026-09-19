package com.aicoach.backend.comparison;

import com.aicoach.backend.enums.ComplianceClassification;
import com.aicoach.backend.enums.DurationType;
import com.aicoach.backend.enums.StepAlignmentSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ActivityComparisonEngine {
    private final ComparisonTolerancePolicy policy;

    public ActivityComparisonEngine(ComparisonTolerancePolicy policy) {
        this.policy = policy;
    }

    public Result compare(Plan plan, Actual actual) {
        List<StepResult> steps = align(plan.steps(), actual);
        List<WeightedScore> scores = new ArrayList<>();
        if (!steps.isEmpty()) {
            double weighted = steps.stream().mapToDouble(s -> s.percentage() * Math.max(1, s.plannedEffort())).sum();
            double weight = steps.stream().mapToDouble(s -> Math.max(1, s.plannedEffort())).sum();
            scores.add(new WeightedScore("etapas", weighted / weight, 0.70));
        }
        if (positive(plan.durationSeconds()) && positive(actual.durationSeconds())) {
            scores.add(new WeightedScore("duração", accuracy(actual.durationSeconds(), plan.durationSeconds(),
                    policy.durationToleranceRatio()), 0.15));
        }
        if (positive(plan.distanceMeters()) && positive(actual.distanceMeters())) {
            scores.add(new WeightedScore("distância", accuracy(actual.distanceMeters(), plan.distanceMeters(),
                    policy.distanceToleranceRatio()), 0.15));
        }
        double percentage = normalize(scores);
        boolean exceeded = exceeds(actual.durationSeconds(), plan.durationSeconds())
                || exceeds(actual.distanceMeters(), plan.distanceMeters());
        ComplianceClassification classification = classify(percentage, exceeded, false);
        String explanation = explanation(scores, percentage, classification);
        return new Result(round(percentage), classification, explanation, steps);
    }

    private List<StepResult> align(List<PlannedStep> planned, Actual actual) {
        if (planned == null || planned.isEmpty()) return List.of();
        if (actual.records() != null && actual.records().size() >= 2) {
            return alignRecords(planned, actual.records());
        }
        if (actual.laps() != null && !actual.laps().isEmpty()) {
            return alignLaps(planned, actual.laps());
        }
        return alignTotal(planned, actual);
    }

    private List<StepResult> alignRecords(List<PlannedStep> planned, List<RecordPoint> source) {
        List<RecordPoint> records = source.stream()
                .filter(r -> r.elapsedSeconds() != null && r.distanceMeters() != null)
                .sorted(Comparator.comparingDouble(RecordPoint::elapsedSeconds)).toList();
        if (records.size() < 2) return List.of();
        List<StepResult> results = new ArrayList<>();
        int startIndex = 0;
        for (PlannedStep step : planned) {
            if (startIndex >= records.size() - 1) {
                results.add(notExecuted(step));
                continue;
            }
            RecordPoint start = records.get(startIndex);
            int endIndex = startIndex + 1;
            while (endIndex < records.size() - 1 && !targetReached(step, start, records.get(endIndex))) endIndex++;
            RecordPoint end = records.get(endIndex);
            List<RecordPoint> slice = records.subList(startIndex, endIndex + 1);
            results.add(result(step, StepAlignmentSource.RECORDS,
                    Math.max(0, end.elapsedSeconds() - start.elapsedSeconds()),
                    Math.max(0, end.distanceMeters() - start.distanceMeters()),
                    average(slice.stream().map(RecordPoint::heartRate).toList()),
                    average(slice.stream().map(RecordPoint::cadence).toList()),
                    start.elapsedSeconds(), end.elapsedSeconds(), start.distanceMeters(), end.distanceMeters()));
            startIndex = endIndex;
        }
        return results;
    }

    private List<StepResult> alignLaps(List<PlannedStep> planned, List<LapSegment> laps) {
        List<StepResult> results = new ArrayList<>();
        int cursor = 0;
        double elapsed = 0;
        double distance = 0;
        for (PlannedStep step : planned) {
            if (cursor >= laps.size()) {
                results.add(notExecuted(step));
                continue;
            }
            double startElapsed = elapsed;
            double startDistance = distance;
            List<Double> hrs = new ArrayList<>();
            List<Double> cadences = new ArrayList<>();
            do {
                LapSegment lap = laps.get(cursor++);
                elapsed += value(lap.durationSeconds());
                distance += value(lap.distanceMeters());
                hrs.add(lap.heartRate());
                cadences.add(lap.cadence());
            } while (cursor < laps.size() && !targetReached(step, startElapsed, startDistance, elapsed, distance));
            results.add(result(step, StepAlignmentSource.LAPS, elapsed - startElapsed, distance - startDistance,
                    average(hrs), average(cadences), startElapsed, elapsed, startDistance, distance));
        }
        return results;
    }

    private List<StepResult> alignTotal(List<PlannedStep> planned, Actual actual) {
        if (!positive(actual.durationSeconds()) && !positive(actual.distanceMeters())) {
            return planned.stream().map(this::notExecuted).toList();
        }
        double plannedEffort = planned.stream().mapToDouble(this::effort).sum();
        double elapsed = 0;
        double distance = 0;
        List<StepResult> results = new ArrayList<>();
        for (PlannedStep step : planned) {
            double share = effort(step) / plannedEffort;
            double duration = value(actual.durationSeconds()) * share;
            double meters = value(actual.distanceMeters()) * share;
            results.add(result(step, StepAlignmentSource.TOTAL, duration, meters,
                    actual.heartRate(), actual.cadence(), elapsed, elapsed + duration,
                    distance, distance + meters));
            elapsed += duration;
            distance += meters;
        }
        return results;
    }

    private StepResult result(PlannedStep step, StepAlignmentSource source, double duration, double distance,
                              Double hr, Double cadence, double startS, double endS,
                              double startM, double endM) {
        double actualPrimary = step.durationType() == DurationType.TIME ? duration : distance;
        double primaryTolerance = step.durationType() == DurationType.TIME
                ? policy.durationToleranceRatio() : policy.distanceToleranceRatio();
        double primaryScore = accuracy(actualPrimary, step.targetValue(), primaryTolerance);
        Double pace = distance > 0 ? duration / distance * 1000.0 : null;
        Double paceScore = paceScore(pace, step.paceFastest(), step.paceSlowest());
        double percentage = paceScore == null ? primaryScore : primaryScore * 0.60 + paceScore * 0.40;
        boolean exceeded = actualPrimary > step.targetValue() * (1.0 + policy.exceededRatio());
        ComplianceClassification classification = classify(percentage, exceeded, actualPrimary <= step.targetValue() * 0.05);
        String detail = String.format(Locale.ROOT,
                "critério=%s; realizado=%.2f; alvo=%d; score_critério=%.2f; score_ritmo=%s; fonte=%s",
                step.durationType(), actualPrimary, step.targetValue(), primaryScore,
                paceScore == null ? "n/a" : String.format(Locale.ROOT, "%.2f", paceScore), source);
        return new StepResult(step.stepId(), step.occurrence(), source, round(percentage), classification,
                duration, distance, pace, hr, cadence, startS, endS, startM, endM, effort(step), detail);
    }

    private StepResult notExecuted(PlannedStep step) {
        return new StepResult(step.stepId(), step.occurrence(), StepAlignmentSource.NONE, 0,
                ComplianceClassification.NOT_EXECUTED, 0, 0, null, null, null,
                null, null, null, null, effort(step), "nenhum intervalo realizado disponível");
    }

    private double effort(PlannedStep step) {
        if (step.durationType() == DurationType.TIME) return Math.max(1, step.targetValue());
        double pace = step.paceFastest() != null && step.paceSlowest() != null
                ? (step.paceFastest() + step.paceSlowest()) / 2.0
                : policy.fallbackPaceSecondsPerKm();
        return Math.max(1, step.targetValue() / 1000.0 * pace);
    }

    private boolean targetReached(PlannedStep step, RecordPoint start, RecordPoint current) {
        return targetReached(step, start.elapsedSeconds(), start.distanceMeters(),
                current.elapsedSeconds(), current.distanceMeters());
    }

    private boolean targetReached(PlannedStep step, double startS, double startM, double endS, double endM) {
        return step.durationType() == DurationType.TIME
                ? endS - startS >= step.targetValue()
                : endM - startM >= step.targetValue();
    }

    private Double paceScore(Double pace, Integer fastest, Integer slowest) {
        if (pace == null || fastest == null || slowest == null || fastest <= 0 || slowest <= 0) return null;
        if (pace >= fastest && pace <= slowest) return 100.0;
        double nearest = pace < fastest ? fastest : slowest;
        return accuracy(pace, nearest, policy.paceToleranceRatio());
    }

    private double accuracy(double actual, double target, double tolerance) {
        if (target <= 0) return 0;
        double deviation = Math.abs(actual - target) / target;
        return round(Math.max(0, 100.0 * (1.0 - Math.max(0, deviation - tolerance))));
    }

    private boolean exceeds(Double actual, Integer target) {
        return positive(actual) && target != null && target > 0 && actual > target * (1.0 + policy.exceededRatio());
    }

    private ComplianceClassification classify(double percentage, boolean exceeded, boolean notExecuted) {
        if (notExecuted) return ComplianceClassification.NOT_EXECUTED;
        if (exceeded) return ComplianceClassification.EXCEEDED;
        if (percentage >= policy.fulfilledMinimum()) return ComplianceClassification.FULFILLED;
        if (percentage >= policy.partialMinimum()) return ComplianceClassification.PARTIAL;
        return ComplianceClassification.DIFFERENT;
    }

    private double normalize(List<WeightedScore> scores) {
        double weights = scores.stream().mapToDouble(WeightedScore::weight).sum();
        return weights == 0 ? 0 : scores.stream().mapToDouble(s -> s.score() * s.weight()).sum() / weights;
    }

    private String explanation(List<WeightedScore> scores, double total, ComplianceClassification classification) {
        String components = scores.stream().map(score -> String.format(Locale.ROOT, "%s=%.2f(peso=%.2f)",
                score.name(), score.score(), score.weight())).reduce((a, b) -> a + "; " + b).orElse("sem dados");
        return String.format(Locale.ROOT, "política=%s; %s; percentual=%.2f; classificação=%s",
                policy.version(), components, total, classification);
    }

    private Double average(List<Double> values) {
        List<Double> present = values.stream().filter(v -> v != null).toList();
        return present.isEmpty() ? null : present.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
    }

    private boolean positive(Number value) { return value != null && value.doubleValue() > 0; }
    private double value(Number value) { return value == null ? 0 : value.doubleValue(); }
    private double round(double value) { return Math.round(value * 100.0) / 100.0; }

    private record WeightedScore(String name, double score, double weight) {}

    public record Plan(Integer durationSeconds, Integer distanceMeters, List<PlannedStep> steps) {}
    public record PlannedStep(Long stepId, int occurrence, DurationType durationType, int targetValue,
                              Integer paceFastest, Integer paceSlowest) {}
    public record Actual(Double durationSeconds, Double distanceMeters, Double heartRate, Double cadence,
                         List<RecordPoint> records, List<LapSegment> laps) {}
    public record RecordPoint(Double elapsedSeconds, Double distanceMeters, Double heartRate, Double cadence) {}
    public record LapSegment(Double durationSeconds, Double distanceMeters, Double heartRate, Double cadence) {}
    public record Result(double percentage, ComplianceClassification classification, String explanation,
                         List<StepResult> steps) {}
    public record StepResult(Long stepId, int occurrence, StepAlignmentSource source, double percentage,
                             ComplianceClassification classification, double durationSeconds, double distanceMeters,
                             Double paceSecondsPerKm, Double heartRate, Double cadence, Double startSeconds,
                             Double endSeconds, Double startMeters, Double endMeters, double plannedEffort,
                             String explanation) {}
}
