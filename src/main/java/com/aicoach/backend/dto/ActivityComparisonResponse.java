package com.aicoach.backend.dto;

import com.aicoach.backend.enums.ActivityMatchType;
import com.aicoach.backend.enums.ComplianceClassification;
import com.aicoach.backend.enums.StepAlignmentSource;
import com.aicoach.backend.enums.DurationType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ActivityComparisonResponse(
        Long id,
        Long athleteId,
        Long activityId,
        Long plannedActivityId,
        ActivityMatchType matchType,
        ComplianceClassification classification,
        BigDecimal compliancePercentage,
        String toleranceVersion,
        Metrics metrics,
        String explanation,
        Instant calculatedAt,
        List<Step> steps) {

    public record Metrics(Integer plannedDurationSeconds, BigDecimal actualDurationSeconds,
                          Integer plannedDistanceMeters, BigDecimal actualDistanceMeters,
                          BigDecimal plannedPaceSecondsPerKm, BigDecimal actualPaceSecondsPerKm,
                          BigDecimal actualAverageHeartRate,
                          BigDecimal actualAverageCadence) {}

    public record Step(Integer sequence, Long workoutStepId, Integer occurrence,
                       DurationType durationType, Integer plannedValue,
                       Integer targetPaceFastestSecondsPerKm, Integer targetPaceSlowestSecondsPerKm,
                       StepAlignmentSource alignmentSource, ComplianceClassification classification,
                       BigDecimal compliancePercentage, BigDecimal actualDurationSeconds,
                       BigDecimal actualDistanceMeters, BigDecimal actualPaceSecondsPerKm,
                       BigDecimal actualAverageHeartRate, BigDecimal actualAverageCadence,
                       BigDecimal intervalStartSeconds, BigDecimal intervalEndSeconds,
                       BigDecimal intervalStartMeters, BigDecimal intervalEndMeters, String explanation) {}
}
