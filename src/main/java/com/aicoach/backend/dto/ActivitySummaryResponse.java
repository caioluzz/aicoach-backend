package com.aicoach.backend.dto;

import java.time.LocalDateTime;

public record ActivitySummaryResponse(
        Long id,
        Long athleteId,
        Long garminActivityId,
        String name,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Double distanceMeters,
        Double durationSeconds,
        String sport,
        Boolean vdotTest,
        Short avgPaceSecondsPerKm,
        Integer averageHeartRate,
        Short avgCadence) {
}
