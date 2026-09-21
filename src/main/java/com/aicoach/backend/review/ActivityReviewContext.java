package com.aicoach.backend.review;

import com.aicoach.backend.dto.ActivityComparisonResponse;

import java.time.LocalDateTime;

public record ActivityReviewContext(
        Long reviewId,
        Long activityId,
        LocalDateTime startedAt,
        String activityName,
        String sport,
        Double durationSeconds,
        Double distanceMeters,
        Integer averageHeartRate,
        Short averageCadence,
        ActivityComparisonResponse deterministicSummary) {
}
