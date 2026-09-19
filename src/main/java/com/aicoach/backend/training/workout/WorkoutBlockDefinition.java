package com.aicoach.backend.training.workout;

import java.util.List;

public record WorkoutBlockDefinition(int repetitions, List<WorkoutStepDefinition> steps) {

    public WorkoutBlockDefinition {
        if (repetitions <= 0 || steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("Workout block requires repetitions and at least one step");
        }
        steps = List.copyOf(steps);
    }
}
