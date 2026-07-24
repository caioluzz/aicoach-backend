package com.aicoach.backend.controller;

import com.aicoach.backend.api.ActivityApi;
import com.aicoach.backend.models.Activity;
import com.aicoach.backend.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ActivityController implements ActivityApi {

    private final ActivityService activityService;

    @Override
    public ResponseEntity<Activity> createActivity(Activity activity) {
        Activity savedActivity = activityService.saveActivity(activity);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedActivity);
    }
}
