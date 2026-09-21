package com.aicoach.backend.dto;

import com.aicoach.backend.enums.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AdaptationDecisionResponse(
        Long id,
        Long athleteId,
        Feedback feedback,
        Evidence evidence,
        String ruleVersion,
        AdaptationAlertLevel alertLevel,
        Integer loadReductionPercent,
        boolean allowIntensity,
        boolean weeklyReviewRequired,
        boolean seasonPlanReviewProposed,
        String materialCause,
        String rationale,
        Instant createdAt,
        List<WorkoutChange> upcomingWorkoutReview) {

    public record Feedback(Long id, Long activityId, LocalDate date, Integer perceivedEffort,
                           Integer fatigue, Double sleepHours, Integer painSeverity,
                           String painLocation, FeedbackFeeling feeling, String comment) {}

    public record Evidence(Long comparisonId, ComplianceClassification complianceClassification,
                           java.math.BigDecimal compliancePercentage, String snapshot) {}

    public record WorkoutChange(Long plannedActivityId, LocalDate scheduledDate,
                                WorkoutAdjustmentAction action, Integer originalDistanceMeters,
                                Integer proposedDistanceMeters, Integer originalDurationSeconds,
                                Integer proposedDurationSeconds, Integer originalStressPoints,
                                Integer proposedStressPoints, String rationale) {}
}
