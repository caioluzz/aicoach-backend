package com.aicoach.backend.service;

import com.aicoach.backend.dto.ConfigurationStatusResponse;
import com.aicoach.backend.dto.GarminCredentialsRequest;
import com.aicoach.backend.dto.IntegrationTestResponse;
import com.aicoach.backend.dto.OpenAiConfigurationRequest;
import com.aicoach.backend.config.OpenAiRuntimeSettingsProvider;
import com.aicoach.backend.models.Athlete;
import com.aicoach.backend.models.AthleteOpenAiSettings;
import com.aicoach.backend.repository.AthleteRepo;
import com.aicoach.backend.repository.AthleteOpenAiSettingsRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ConfigurationService {
    private final AthleteRepo athleteRepo;
    private final AthleteOpenAiSettingsRepo openAiSettingsRepo;
    private final OpenAiRuntimeSettingsProvider openAiSettingsProvider;
    private final Clock clock;

    @Value("${openai.api.url:https://api.openai.com/v1}")
    private String openAiUrl;
    @Value("${openai.timeout-seconds:60}")
    private int timeoutSeconds;

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

    @Transactional
    public ConfigurationStatusResponse updateOpenAi(Long athleteId, OpenAiConfigurationRequest request) {
        Athlete athlete = athleteRepo.findById(athleteId)
                .orElseThrow(() -> new AthleteNotFoundException(athleteId));
        AthleteOpenAiSettings settings = openAiSettingsRepo.findById(athleteId)
                .orElseGet(AthleteOpenAiSettings::new);
        settings.setAthlete(athlete);
        settings.setApiKey(request.apiKey().trim());
        settings.setPlannerModel(request.plannerModel().trim());
        settings.setWeeklyPlannerModel(request.weeklyPlannerModel().trim());
        settings.setActivityReviewModel(request.activityReviewModel().trim());
        settings.setMaxOutputTokens(request.maxOutputTokens());
        settings.setWeeklyMaxOutputTokens(request.weeklyMaxOutputTokens());
        settings.setActivityReviewMaxOutputTokens(request.activityReviewMaxOutputTokens());
        settings.setLastValidatedAt(null);
        settings.setUpdatedAt(clock.instant());
        openAiSettingsRepo.save(settings);
        return response(athlete);
    }

    @Transactional
    public IntegrationTestResponse testOpenAi(Long athleteId) {
        athleteRepo.findById(athleteId).orElseThrow(() -> new AthleteNotFoundException(athleteId));
        OpenAiRuntimeSettingsProvider.Settings settings = openAiSettingsProvider.resolve(athleteId);
        if (settings.apiKey() == null || settings.apiKey().isBlank()) {
            return new IntegrationTestResponse(false, "Configure a chave da OpenAI antes do teste.", null);
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(Math.min(timeoutSeconds, 20)));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        try {
            RestClient.builder().baseUrl(openAiUrl).requestFactory(factory).build().get()
                    .uri("/models/{model}", settings.plannerModel())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + settings.apiKey())
                    .retrieve().toBodilessEntity();
            Instant validatedAt = clock.instant();
            openAiSettingsRepo.findById(athleteId).ifPresent(value -> {
                value.setLastValidatedAt(validatedAt);
                value.setUpdatedAt(validatedAt);
                openAiSettingsRepo.save(value);
            });
            return new IntegrationTestResponse(true, "Conexão com a OpenAI validada.", validatedAt);
        } catch (RestClientResponseException exception) {
            return new IntegrationTestResponse(false,
                    "A OpenAI rejeitou a validação (HTTP " + exception.getStatusCode().value() + ").", null);
        } catch (RuntimeException exception) {
            return new IntegrationTestResponse(false, "Não foi possível alcançar a OpenAI: " + exception.getMessage(), null);
        }
    }

    private ConfigurationStatusResponse response(Athlete athlete) {
        String email = athlete.getGarminEmail();
        AthleteOpenAiSettings saved = openAiSettingsRepo.findById(athlete.getId()).orElse(null);
        OpenAiRuntimeSettingsProvider.Settings runtime = openAiSettingsProvider.resolve(athlete.getId());
        String effectiveKey = runtime.apiKey();
        return new ConfigurationStatusResponse(
                athlete.getId(),
                new ConfigurationStatusResponse.IntegrationStatus(
                        email != null && !email.isBlank() && athlete.getGarminPassword() != null
                                && !athlete.getGarminPassword().isBlank(),
                        maskEmail(email), null),
                new ConfigurationStatusResponse.OpenAiStatus(
                        effectiveKey != null && !effectiveKey.isBlank(),
                        maskSecret(effectiveKey), saved == null ? null : saved.getLastValidatedAt(),
                        runtime.plannerModel(), runtime.weeklyPlannerModel(), runtime.activityReviewModel(),
                        runtime.maxOutputTokens(), runtime.weeklyMaxOutputTokens(),
                        runtime.activityReviewMaxOutputTokens(), saved == null ? "ENVIRONMENT" : "DATABASE"));
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
