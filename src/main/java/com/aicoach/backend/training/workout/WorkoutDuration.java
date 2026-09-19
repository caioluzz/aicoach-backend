package com.aicoach.backend.training.workout;

public record WorkoutDuration(Unit unit, int value) {

    public enum Unit {
        SECONDS,
        METERS
    }

    public WorkoutDuration {
        if (unit == null || value <= 0) {
            throw new IllegalArgumentException("Workout duration requires a unit and positive value");
        }
    }
}
