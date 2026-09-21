package com.aicoach.backend.dto;

import java.time.Instant;

public record ConfigurationStatusResponse(
        Long athleteId,
        IntegrationStatus garmin,
        OpenAiStatus openai) {

    public record IntegrationStatus(boolean configured, String maskedHint, Instant lastValidatedAt) {}

    public record OpenAiStatus(boolean configured, String maskedHint, Instant lastValidatedAt,
                               String plannerModel, String weeklyPlannerModel, String activityReviewModel) {}
}
