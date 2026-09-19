package com.aicoach.backend.api;

import com.aicoach.backend.models.Activity;
import com.aicoach.backend.dto.ActivitySyncResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

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
}
