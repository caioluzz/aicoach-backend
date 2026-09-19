package com.aicoach.backend.controller;

import com.aicoach.backend.api.SeasonPlanApi;
import com.aicoach.backend.dto.*;
import com.aicoach.backend.service.SeasonPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SeasonPlanController implements SeasonPlanApi {
    private final SeasonPlanService service;

    public SeasonPlanResponse create(Long athleteId, SeasonPlanCreateRequest request) {
        return service.create(athleteId, request);
    }

    public SeasonPlanResponse get(Long athleteId, Long planId) {
        return service.get(athleteId, planId);
    }

    public SeasonPlanResponse getLatest(Long athleteId) {
        return service.getLatest(athleteId);
    }

    public List<SeasonPlanResponse> getHistory(Long athleteId) {
        return service.getHistory(athleteId);
    }

    public SeasonPlanResponse review(Long athleteId, Long planId, SeasonPlanReviewRequest request) {
        return service.review(athleteId, planId, request);
    }
}
