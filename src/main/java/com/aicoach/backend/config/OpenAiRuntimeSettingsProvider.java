package com.aicoach.backend.config;

public interface OpenAiRuntimeSettingsProvider {
    Settings resolve(Long athleteId);

    record Settings(String apiKey, String plannerModel, String weeklyPlannerModel,
                    String activityReviewModel, int maxOutputTokens,
                    int weeklyMaxOutputTokens, int activityReviewMaxOutputTokens) {
    }
}
