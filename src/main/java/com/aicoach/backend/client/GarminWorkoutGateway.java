package com.aicoach.backend.client;

import com.aicoach.backend.dto.GarminWorkoutContract;

import java.time.LocalDate;
import java.util.Map;

public interface GarminWorkoutGateway {
    Preview preview(GarminWorkoutContract workout);
    Delivery deliver(Credentials credentials, String idempotencyKey, GarminWorkoutContract workout);
    Delivery update(Credentials credentials, Long workoutId, Long scheduledWorkoutId,
                    String idempotencyKey, GarminWorkoutContract workout);
    Confirmation confirm(Credentials credentials, Long workoutId, Long scheduledWorkoutId,
                         LocalDate scheduledDate, String idempotencyKey);
    void cancel(Credentials credentials, Long workoutId, Long scheduledWorkoutId);

    record Credentials(String email, String password) {}
    record Preview(String hash, String idempotencyKey, LocalDate scheduledDate, Map<String, Object> payload) {}
    record Delivery(Long workoutId, Long scheduledWorkoutId, LocalDate scheduledDate,
                    String idempotencyKey, Boolean reusedWorkout) {}
    record Confirmation(Boolean confirmed, Long workoutId, Long scheduledWorkoutId, LocalDate scheduledDate) {}
}
