package com.aicoach.backend.config;

import com.aicoach.backend.models.AthleteOpenAiSettings;
import com.aicoach.backend.repository.AthleteOpenAiSettingsRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatabaseOpenAiRuntimeSettingsProvider implements OpenAiRuntimeSettingsProvider {
    private final AthleteOpenAiSettingsRepo repository;

    @Value("${openai.api.key:}") private String defaultApiKey;
    @Value("${openai.model.planner:gpt-5-mini}") private String defaultPlannerModel;
    @Value("${openai.model.weekly-planner:gpt-5-mini}") private String defaultWeeklyPlannerModel;
    @Value("${openai.model.activity-review:gpt-5-mini}") private String defaultActivityReviewModel;
    @Value("${openai.max-output-tokens:8000}") private int defaultMaxOutputTokens;
    @Value("${openai.weekly.max-output-tokens:12000}") private int defaultWeeklyMaxOutputTokens;
    @Value("${openai.activity-review.max-output-tokens:2500}") private int defaultActivityReviewMaxOutputTokens;

    @Override
    public Settings resolve(Long athleteId) {
        return repository.findById(athleteId).map(this::fromEntity).orElseGet(this::defaults);
    }

    private Settings fromEntity(AthleteOpenAiSettings value) {
        return new Settings(value.getApiKey(), value.getPlannerModel(), value.getWeeklyPlannerModel(),
                value.getActivityReviewModel(), value.getMaxOutputTokens(), value.getWeeklyMaxOutputTokens(),
                value.getActivityReviewMaxOutputTokens());
    }

    private Settings defaults() {
        return new Settings(defaultApiKey, defaultPlannerModel, defaultWeeklyPlannerModel,
                defaultActivityReviewModel, defaultMaxOutputTokens, defaultWeeklyMaxOutputTokens,
                defaultActivityReviewMaxOutputTokens);
    }
}
