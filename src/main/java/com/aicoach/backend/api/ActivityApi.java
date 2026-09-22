package com.aicoach.backend.api;

import com.aicoach.backend.models.Activity;
import com.aicoach.backend.dto.ActivitySyncResponse;
import com.aicoach.backend.dto.ActivityComparisonResponse;
import com.aicoach.backend.dto.ActivityReviewResponse;
import com.aicoach.backend.dto.ActivitySummaryResponse;
import com.aicoach.backend.dto.VdotTestConfirmationResponse;
import com.aicoach.backend.dto.GarminActivityImportRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

import java.util.List;

@RequestMapping("/api/v1/activities")
public interface ActivityApi {

    @PostMapping
    ResponseEntity<Activity> createActivity(@RequestBody Activity activity);

    @PostMapping("/sync")
    ResponseEntity<List<ActivitySyncResponse>> syncAll();

    @PostMapping("/sync/{athleteId}")
    ResponseEntity<ActivitySyncResponse> syncAthlete(@PathVariable Long athleteId);

    @GetMapping("/sync/status")
    ResponseEntity<List<ActivitySyncResponse>> getSyncStatuses();

    @GetMapping("/sync/status/{athleteId}")
    ResponseEntity<ActivitySyncResponse> getSyncStatus(@PathVariable Long athleteId);

    @PostMapping("/{activityId}/comparison")
    ResponseEntity<ActivityComparisonResponse> compareActivity(@PathVariable Long activityId);

    @GetMapping("/{activityId}/comparison")
    ResponseEntity<ActivityComparisonResponse> getComparison(@PathVariable Long activityId);

    @PostMapping("/{activityId}/review")
    ResponseEntity<ActivityReviewResponse> reviewActivity(@PathVariable Long activityId);

    @GetMapping("/{activityId}/review")
    ResponseEntity<ActivityReviewResponse> getActivityReview(@PathVariable Long activityId);

    @GetMapping("/comparisons/athletes/{athleteId}")
    ResponseEntity<List<ActivityComparisonResponse>> listComparisons(@PathVariable Long athleteId);

    @PostMapping("/comparisons/athletes/{athleteId}/reconcile")
    ResponseEntity<List<ActivityComparisonResponse>> reconcileComparisons(
            @PathVariable Long athleteId, @RequestParam(required = false) LocalDate throughDate);

    @GetMapping("/athletes/{athleteId}")
    ResponseEntity<List<ActivitySummaryResponse>> listActivities(@PathVariable Long athleteId);

    @PostMapping("/athletes/{athleteId}/import")
    ResponseEntity<ActivitySummaryResponse> importActivity(@PathVariable Long athleteId,
                                                            @Valid @RequestBody GarminActivityImportRequest request);

    @PostMapping("/athletes/{athleteId}/{activityId}/vdot-test/confirm")
    ResponseEntity<VdotTestConfirmationResponse> confirmVdotTest(@PathVariable Long athleteId,
                                                                  @PathVariable Long activityId);
}
