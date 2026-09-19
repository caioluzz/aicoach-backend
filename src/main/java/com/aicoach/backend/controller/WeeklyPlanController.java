package com.aicoach.backend.controller;

import com.aicoach.backend.api.WeeklyPlanApi;
import com.aicoach.backend.dto.*;
import com.aicoach.backend.service.WeeklyPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class WeeklyPlanController implements WeeklyPlanApi {
    private final WeeklyPlanService service;

    public WeeklyPlanResponse create(Long athleteId, WeeklyPlanCreateRequest request) {
        return service.create(athleteId, request);
    }

    public WeeklyPlanResponse get(Long athleteId, Long weeklyPlanId) {
        return service.get(athleteId, weeklyPlanId);
    }

    public WeeklyPlanResponse getLatest(Long athleteId, Long seasonPlanId, Integer weekNumber) {
        return service.getLatest(athleteId, seasonPlanId, weekNumber);
    }

    public List<WeeklyPlanResponse> getHistory(Long athleteId, Long seasonPlanId, Integer weekNumber) {
        return service.getHistory(athleteId, seasonPlanId, weekNumber);
    }

    public WeeklyPlanResponse review(Long athleteId, Long weeklyPlanId, WeeklyPlanReviewRequest request) {
        return service.review(athleteId, weeklyPlanId, request);
    }

    public WeeklyPlanResponse regenerate(Long athleteId, Long weeklyPlanId, WeeklyPlanRegenerateRequest request) {
        return service.regenerate(athleteId, weeklyPlanId, request);
    }

    public WeeklyPlanResponse edit(Long athleteId, Long weeklyPlanId, WeeklyPlanEditRequest request) {
        return service.edit(athleteId, weeklyPlanId, request);
    }
}
