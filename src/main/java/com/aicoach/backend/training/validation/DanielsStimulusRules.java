package com.aicoach.backend.training.validation;

import com.aicoach.backend.training.daniels.DanielsIntensity;

import java.util.ArrayList;
import java.util.List;

public final class DanielsStimulusRules {

    public List<ValidationIssue> validateRepetition(
            DanielsIntensity intensity,
            int workSeconds,
            int recoverySeconds,
            DanielsIntensity recoveryIntensity) {
        List<ValidationIssue> issues = new ArrayList<>();
        if (intensity == DanielsIntensity.I) {
            if (workSeconds < 30 || workSeconds > 300) {
                issues.add(new ValidationIssue("I_WORK_DURATION", "I repetitions must last 30 seconds to five minutes"));
            }
            if (recoverySeconds > workSeconds) {
                issues.add(new ValidationIssue("I_RECOVERY_DURATION", "I recovery cannot exceed the preceding work interval"));
            }
            if (recoveryIntensity != DanielsIntensity.E) {
                issues.add(new ValidationIssue("I_RECOVERY_TYPE", "I recovery must be active at E intensity"));
            }
        } else if (intensity == DanielsIntensity.T) {
            if (workSeconds < 180 || workSeconds > 900) {
                issues.add(new ValidationIssue("T_WORK_DURATION", "T cruise intervals must last three to fifteen minutes"));
            }
            if (recoverySeconds != Math.round(workSeconds / 5.0f)) {
                issues.add(new ValidationIssue("T_RECOVERY_DURATION", "T cruise recovery must equal one fifth of work time"));
            }
        } else if (intensity == DanielsIntensity.R) {
            if (workSeconds > 120) {
                issues.add(new ValidationIssue("R_WORK_DURATION", "R repetitions cannot exceed two minutes"));
            }
            if (recoverySeconds < workSeconds * 2 || recoverySeconds > workSeconds * 4) {
                issues.add(new ValidationIssue("R_RECOVERY_DURATION", "R recovery must be two to four times work duration"));
            }
        }
        return List.copyOf(issues);
    }

    public List<ValidationIssue> validateContinuous(DanielsIntensity intensity, int durationSeconds, int distanceMeters,
            int weeklyDurationSeconds, int weeklyDistanceMeters) {
        List<ValidationIssue> issues = new ArrayList<>();
        if (intensity == DanielsIntensity.E) {
            if (durationSeconds > 9_000 || distanceMeters > weeklyDistanceMeters * 30 / 100) {
                issues.add(new ValidationIssue("E_LONG_RUN_LIMIT", "Long E running cannot exceed 150 minutes or 30% of weekly distance"));
            }
        } else if (intensity == DanielsIntensity.M) {
            if (durationSeconds > 9_000 || distanceMeters > 25_750) {
                issues.add(new ValidationIssue("M_LIMIT", "M running cannot exceed 150 minutes or 16 miles"));
            }
        } else if (intensity == DanielsIntensity.T) {
            if (durationSeconds < 1_200 || durationSeconds > 3_600) {
                issues.add(new ValidationIssue("T_CONTINUOUS_DURATION", "Continuous T running must last 20 to 60 minutes"));
            }
            if (durationSeconds > weeklyDurationSeconds / 10 || distanceMeters > weeklyDistanceMeters / 10) {
                issues.add(new ValidationIssue("T_WEEKLY_LIMIT", "T running cannot exceed 10% of weekly time or distance"));
            }
        }
        return List.copyOf(issues);
    }
}
