package com.aicoach.backend.service;

import com.aicoach.backend.dto.ConfigurationStatusResponse;
import com.aicoach.backend.dto.GarminCredentialsRequest;
import com.aicoach.backend.models.Athlete;
import com.aicoach.backend.repository.AthleteRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ConfigurationService {
    private final AthleteRepo athleteRepo;

    @Value("${openai.api.key:}")
    private String openAiKey;
    @Value("${openai.model.planner:gpt-5-mini}")
    private String plannerModel;
    @Value("${openai.model.weekly-planner:gpt-5-mini}")
    private String weeklyPlannerModel;
    @Value("${openai.model.activity-review:gpt-5-mini}")
    private String activityReviewModel;

    @Transactional(readOnly = true)
    public ConfigurationStatusResponse get(Long athleteId) {
        Athlete athlete = athleteRepo.findById(athleteId)
                .orElseThrow(() -> new AthleteNotFoundException(athleteId));
        return response(athlete);
    }

    @Transactional
    public ConfigurationStatusResponse updateGarmin(Long athleteId, GarminCredentialsRequest request) {
        Athlete athlete = athleteRepo.findById(athleteId)
                .orElseThrow(() -> new AthleteNotFoundException(athleteId));
        athlete.setGarminEmail(request.email().trim().toLowerCase(Locale.ROOT));
        athlete.setGarminPassword(request.password());
        return response(athleteRepo.save(athlete));
    }

    private ConfigurationStatusResponse response(Athlete athlete) {
        String email = athlete.getGarminEmail();
        return new ConfigurationStatusResponse(
                athlete.getId(),
                new ConfigurationStatusResponse.IntegrationStatus(
                        email != null && !email.isBlank() && athlete.getGarminPassword() != null
                                && !athlete.getGarminPassword().isBlank(),
                        maskEmail(email), null),
                new ConfigurationStatusResponse.OpenAiStatus(
                        openAiKey != null && !openAiKey.isBlank(),
                        maskSecret(openAiKey), null, plannerModel, weeklyPlannerModel, activityReviewModel));
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) return null;
        int at = email.indexOf('@');
        if (at <= 1) return "••••" + (at >= 0 ? email.substring(at) : "");
        return email.charAt(0) + "••••" + email.substring(at);
    }

    private String maskSecret(String secret) {
        if (secret == null || secret.isBlank()) return null;
        int visible = Math.min(4, secret.length());
        return "••••" + secret.substring(secret.length() - visible);
    }
}
