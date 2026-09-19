package com.aicoach.backend.training.validation;

import com.aicoach.backend.training.daniels.DanielsIntensity;
import com.aicoach.backend.training.validation.TrainingLoad.SessionLoad;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class DanielsTrainingValidator {

    public List<ValidationIssue> validateWeek(TrainingLoad load) {
        List<ValidationIssue> issues = new ArrayList<>();
        validateProgression(load, issues);
        validateDistribution(load, issues);
        validateRecovery(load, issues);
        validateIntensityLimits(load, issues);
        return List.copyOf(issues);
    }

    private static void validateProgression(TrainingLoad load, List<ValidationIssue> issues) {
        boolean distanceIncreased = load.weeklyDistanceMeters() > load.priorWeeklyDistanceMeters();
        boolean durationIncreased = load.weeklyDurationSeconds() > load.priorWeeklyDurationSeconds();
        if ((distanceIncreased || durationIncreased) && load.stableWeeksAtPriorLoad() < 3) {
            issues.add(new ValidationIssue("PROGRESSION_STABILITY",
                    "Keep the previous training load for at least three weeks before increasing it"));
        }

        int distanceIncrease = load.weeklyDistanceMeters() - load.priorWeeklyDistanceMeters();
        int maxDistanceIncrease = Math.min(load.sessionsPerWeek() * 1_500, 15_000);
        if (distanceIncrease > maxDistanceIncrease) {
            issues.add(new ValidationIssue("PROGRESSION_DISTANCE",
                    "Weekly distance increase exceeds 1.5 km per session or the 15 km ceiling"));
        }

        int durationIncrease = load.weeklyDurationSeconds() - load.priorWeeklyDurationSeconds();
        int maxDurationIncrease = Math.min(load.sessionsPerWeek() * 360, 3_600);
        if (durationIncrease > maxDurationIncrease) {
            issues.add(new ValidationIssue("PROGRESSION_DURATION",
                    "Weekly duration increase exceeds six minutes per session or the 60 minute ceiling"));
        }
    }

    private static void validateDistribution(TrainingLoad load, List<ValidationIssue> issues) {
        int total = load.sessions().stream().mapToInt(SessionLoad::durationSeconds).sum();
        if (total == 0) {
            return;
        }
        int easyAndMarathon = load.sessions().stream()
                .filter(session -> session.intensity() == DanielsIntensity.E
                        || session.intensity() == DanielsIntensity.M)
                .mapToInt(SessionLoad::durationSeconds)
                .sum();
        if (easyAndMarathon < Math.ceil(total * 0.80)) {
            issues.add(new ValidationIssue("INTENSITY_DISTRIBUTION",
                    "At least 80% of weekly running time must be at E or M intensity"));
        }
    }

    private static void validateRecovery(TrainingLoad load, List<ValidationIssue> issues) {
        List<SessionLoad> quality = load.sessions().stream()
                .filter(session -> session.intensity() == DanielsIntensity.T
                        || session.intensity() == DanielsIntensity.I
                        || session.intensity() == DanielsIntensity.R)
                .sorted(Comparator.comparing(SessionLoad::date))
                .toList();
        for (int index = 1; index < quality.size(); index++) {
            long gap = ChronoUnit.DAYS.between(quality.get(index - 1).date(), quality.get(index).date());
            if (gap < 2) {
                issues.add(new ValidationIssue("QUALITY_RECOVERY",
                        "Quality sessions require at least one intervening recovery day"));
                return;
            }
        }
    }

    private static void validateIntensityLimits(TrainingLoad load, List<ValidationIssue> issues) {
        int thresholdSeconds = totalDuration(load, DanielsIntensity.T);
        int intervalSeconds = totalDuration(load, DanielsIntensity.I);
        int intervalMeters = totalDistance(load, DanielsIntensity.I);
        int repetitionSeconds = totalDuration(load, DanielsIntensity.R);
        int repetitionMeters = totalDistance(load, DanielsIntensity.R);

        if (thresholdSeconds > Math.min(3_600, load.weeklyDurationSeconds() / 10)) {
            issues.add(new ValidationIssue("THRESHOLD_LIMIT", "T running exceeds 10% of weekly time or 60 minutes"));
        }
        if (intervalSeconds > Math.min(1_800, load.weeklyDurationSeconds() * 8 / 100)
                || intervalMeters > Math.min(10_000, load.weeklyDistanceMeters() * 8 / 100)) {
            issues.add(new ValidationIssue("INTERVAL_LIMIT", "I running exceeds 8% of weekly load, 30 minutes or 10 km"));
        }
        if (repetitionSeconds > Math.min(1_200, load.weeklyDurationSeconds() * 3 / 100)
                || repetitionMeters > Math.min(8_000, load.weeklyDistanceMeters() * 5 / 100)) {
            issues.add(new ValidationIssue("REPETITION_LIMIT", "R running exceeds 3% of weekly time, 5% of distance, 20 minutes or 8 km"));
        }
    }

    private static int totalDuration(TrainingLoad load, DanielsIntensity intensity) {
        return load.sessions().stream().filter(session -> session.intensity() == intensity)
                .mapToInt(SessionLoad::durationSeconds).sum();
    }

    private static int totalDistance(TrainingLoad load, DanielsIntensity intensity) {
        return load.sessions().stream().filter(session -> session.intensity() == intensity)
                .mapToInt(SessionLoad::distanceMeters).sum();
    }
}
