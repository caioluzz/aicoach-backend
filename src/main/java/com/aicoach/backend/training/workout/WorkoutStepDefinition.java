package com.aicoach.backend.training.workout;

import com.aicoach.backend.training.daniels.DanielsIntensity;
import com.aicoach.backend.training.daniels.PaceRange;

public record WorkoutStepDefinition(
        Kind kind,
        WorkoutDuration duration,
        DanielsIntensity intensity,
        PaceRange targetPace,
        String instruction) {

    public enum Kind {
        WARMUP,
        WORK,
        RECOVERY,
        COOLDOWN
    }

    public WorkoutStepDefinition {
        if (kind == null || duration == null) {
            throw new IllegalArgumentException("Workout step requires kind and duration");
        }
        if (kind != Kind.RECOVERY && intensity == null) {
            throw new IllegalArgumentException("Non-recovery step requires an intensity");
        }
        if (targetPace != null && intensity == null) {
            throw new IllegalArgumentException("A target pace requires an intensity");
        }
    }
}
