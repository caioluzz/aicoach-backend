package com.aicoach.backend.controller;

import com.aicoach.backend.api.ActivityApi;
import com.aicoach.backend.models.Activity;
import com.aicoach.backend.dto.ActivitySyncResponse;
import com.aicoach.backend.dto.ActivityComparisonResponse;
import com.aicoach.backend.dto.ActivityReviewResponse;
import com.aicoach.backend.dto.ActivitySummaryResponse;
import com.aicoach.backend.dto.VdotTestConfirmationResponse;
import com.aicoach.backend.dto.GarminActivityImportRequest;
import com.aicoach.backend.service.ActivityComparisonService;
import com.aicoach.backend.service.ActivityReviewService;
import com.aicoach.backend.service.ActivityService;
import com.aicoach.backend.client.GarminAdapterException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatusCode;

import java.util.List;
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class ActivityController implements ActivityApi {

    private final ActivityService activityService;
    private final ActivityComparisonService activityComparisonService;
    private final ActivityReviewService activityReviewService;

    @Override
    public ResponseEntity<Activity> createActivity(Activity activity) {
        Activity savedActivity = activityService.saveActivity(activity);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedActivity);
    }

    @Override
    public ResponseEntity<List<ActivitySyncResponse>> syncAll() {
        return ResponseEntity.ok(activityService.syncAllAthletes());
    }

    @Override
    public ResponseEntity<ActivitySyncResponse> syncAthlete(Long athleteId) {
        return ResponseEntity.ok(activityService.syncGarminActivities(athleteId));
    }

    @Override
    public ResponseEntity<List<ActivitySyncResponse>> getSyncStatuses() {
        return ResponseEntity.ok(activityService.getSyncStatuses());
    }

    @Override
    public ResponseEntity<ActivitySyncResponse> getSyncStatus(Long athleteId) {
        return ResponseEntity.ok(activityService.getSyncStatus(athleteId));
    }

    @Override
    public ResponseEntity<ActivityComparisonResponse> compareActivity(Long activityId) {
        return ResponseEntity.ok(activityComparisonService.compareActivity(activityId));
    }

    @Override
    public ResponseEntity<ActivityComparisonResponse> getComparison(Long activityId) {
        return ResponseEntity.ok(activityComparisonService.getForActivity(activityId));
    }

    @Override
    public ResponseEntity<ActivityReviewResponse> reviewActivity(Long activityId) {
        return ResponseEntity.ok(activityReviewService.review(activityId));
    }

    @Override
    public ResponseEntity<ActivityReviewResponse> getActivityReview(Long activityId) {
        return ResponseEntity.ok(activityReviewService.getForActivity(activityId));
    }

    @Override
    public ResponseEntity<List<ActivityComparisonResponse>> listComparisons(Long athleteId) {
        return ResponseEntity.ok(activityComparisonService.listForAthlete(athleteId));
    }

    @Override
    public ResponseEntity<List<ActivityComparisonResponse>> reconcileComparisons(Long athleteId, LocalDate throughDate) {
        return ResponseEntity.ok(activityComparisonService.reconcileMissed(athleteId, throughDate));
    }

    @Override
    public ResponseEntity<List<ActivitySummaryResponse>> listActivities(Long athleteId) {
        return ResponseEntity.ok(activityService.listActivities(athleteId));
    }

    @Override
    public ResponseEntity<ActivitySummaryResponse> importActivity(Long athleteId, GarminActivityImportRequest request) {
        try {
            return ResponseEntity.ok(activityService.importGarminActivity(athleteId, request.garminActivityId()));
        } catch (GarminAdapterException exception) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(exception.getStatusCode()),
                    exception.getMessage(), exception);
        }
    }

    @Override
    public ResponseEntity<VdotTestConfirmationResponse> confirmVdotTest(Long athleteId, Long activityId) {
        return ResponseEntity.ok(activityService.confirmVdotTest(athleteId, activityId));
    }
}
