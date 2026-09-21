package com.aicoach.backend.dto;

import com.aicoach.backend.enums.SegmentQueryType;
import com.aicoach.backend.enums.SegmentResolution;
import com.aicoach.backend.enums.TelemetryField;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ActivitySegmentDetails(
        Long activityId,
        SegmentQueryType queryType,
        BigDecimal requestedStart,
        BigDecimal requestedEnd,
        BigDecimal returnedStart,
        BigDecimal returnedEnd,
        SegmentResolution resolution,
        List<TelemetryField> fields,
        String reason,
        List<Step> steps,
        List<Point> points) {

    public record Step(Integer sequence, BigDecimal startSeconds, BigDecimal endSeconds,
                       BigDecimal startMeters, BigDecimal endMeters, String classification,
                       BigDecimal compliancePercentage, BigDecimal paceSecondsPerKm,
                       BigDecimal averageHeartRate, BigDecimal averageCadence, String explanation) {
    }

    public record Point(LocalDateTime timestamp, BigDecimal elapsedSeconds, BigDecimal distanceKilometers,
                        BigDecimal paceSecondsPerKm, BigDecimal heartRate,
                        BigDecimal cadence, BigDecimal altitudeMeters) {
    }
}
