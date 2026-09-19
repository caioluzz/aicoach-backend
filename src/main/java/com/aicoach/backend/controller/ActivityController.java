package com.aicoach.backend.controller;

import com.aicoach.backend.api.ActivityApi;
import com.aicoach.backend.models.Activity;
import com.aicoach.backend.dto.ActivitySyncResponse;
import com.aicoach.backend.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ActivityController implements ActivityApi {

    private final ActivityService activityService;

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
}
