package com.aicoach.backend.dto;

import com.aicoach.backend.enums.GarminDeliveryStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record GarminDeliveryResponse(
        Long weeklyPlanId,
        Integer planVersion,
        List<Item> deliveries) {
    public record Item(
            Long id,
            Long plannedActivityId,
            Integer sessionOrder,
            String name,
            GarminDeliveryStatus status,
            String contentHash,
            LocalDate scheduledDate,
            Long externalWorkoutId,
            Long externalScheduleId,
            Integer attemptCount,
            String lastError,
            Instant updatedAt,
            Instant confirmedAt,
            Instant cancelledAt) {}
}
