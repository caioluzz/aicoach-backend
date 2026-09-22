package com.aicoach.backend.dto;

import java.time.LocalDate;
import java.util.Map;

public record VdotTestWorkoutResponse(
        Long athleteId,
        String name,
        LocalDate scheduledDate,
        String idempotencyKey,
        Map<String, Object> payload,
        Delivery delivery) {

    public record Delivery(Long workoutId, Long scheduledWorkoutId, Boolean reusedWorkout) {}
}
