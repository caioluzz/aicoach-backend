package com.aicoach.backend.dto;

import java.time.Instant;
import java.util.Map;

public record VdotTestConfirmationResponse(
        Long activityId,
        Long athleteId,
        Integer distanceMeters,
        Integer durationSeconds,
        PerformanceProfile profile) {

    public record PerformanceProfile(
            Double vdot,
            String engineVersion,
            String coefficientSet,
            Instant calculatedAt,
            Map<String, PaceRange> paces) {}

    public record PaceRange(Integer fastestSecondsPerKm, Integer slowestSecondsPerKm) {}
}
