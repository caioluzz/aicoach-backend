package com.aicoach.backend.controller;

import com.aicoach.backend.api.WeeklyPlanApi;
import com.aicoach.backend.dto.*;
import com.aicoach.backend.service.GarminWorkoutDeliveryService;
import com.aicoach.backend.service.WeeklyPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class WeeklyPlanController implements WeeklyPlanApi {
    private final WeeklyPlanService service;
    private final GarminWorkoutDeliveryService garminDeliveryService;

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

    public GarminPreviewResponse previewGarmin(Long athleteId, Long weeklyPlanId) {
        return garminDeliveryService.preview(athleteId, weeklyPlanId);
    }

    public GarminDeliveryResponse deliverGarmin(Long athleteId, Long weeklyPlanId) {
        return garminDeliveryService.deliver(athleteId, weeklyPlanId);
    }

    public GarminDeliveryResponse confirmGarmin(Long athleteId, Long weeklyPlanId) {
        return garminDeliveryService.confirm(athleteId, weeklyPlanId);
    }

    public GarminDeliveryResponse updateGarmin(Long athleteId, Long weeklyPlanId, Long deliveryId) {
        return garminDeliveryService.update(athleteId, weeklyPlanId, deliveryId);
    }

    public GarminDeliveryResponse cancelGarmin(Long athleteId, Long weeklyPlanId, Long deliveryId) {
        return garminDeliveryService.cancel(athleteId, weeklyPlanId, deliveryId);
    }
}
