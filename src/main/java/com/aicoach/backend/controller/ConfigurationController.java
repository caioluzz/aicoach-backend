package com.aicoach.backend.controller;

import com.aicoach.backend.api.ConfigurationApi;
import com.aicoach.backend.dto.ConfigurationStatusResponse;
import com.aicoach.backend.dto.GarminCredentialsRequest;
import com.aicoach.backend.dto.IntegrationTestResponse;
import com.aicoach.backend.dto.OpenAiConfigurationRequest;
import com.aicoach.backend.service.ConfigurationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ConfigurationController implements ConfigurationApi {
    private final ConfigurationService service;

    @Override
    public ConfigurationStatusResponse get(Long athleteId) {
        return service.get(athleteId);
    }

    @Override
    public ConfigurationStatusResponse updateGarmin(Long athleteId, GarminCredentialsRequest request) {
        return service.updateGarmin(athleteId, request);
    }

    @Override
    public ConfigurationStatusResponse updateOpenAi(Long athleteId, OpenAiConfigurationRequest request) {
        return service.updateOpenAi(athleteId, request);
    }

    @Override
    public IntegrationTestResponse testOpenAi(Long athleteId) {
        return service.testOpenAi(athleteId);
    }
}
