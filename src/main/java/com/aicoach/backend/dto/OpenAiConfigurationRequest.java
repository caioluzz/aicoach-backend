package com.aicoach.backend.dto;

import jakarta.validation.constraints.*;

public record OpenAiConfigurationRequest(
        @NotBlank @Size(max = 500) String apiKey,
        @NotBlank @Size(max = 100) String plannerModel,
        @NotBlank @Size(max = 100) String weeklyPlannerModel,
        @NotBlank @Size(max = 100) String activityReviewModel,
        @NotNull @Min(500) @Max(100000) Integer maxOutputTokens,
        @NotNull @Min(500) @Max(100000) Integer weeklyMaxOutputTokens,
        @NotNull @Min(500) @Max(100000) Integer activityReviewMaxOutputTokens
) {
}
