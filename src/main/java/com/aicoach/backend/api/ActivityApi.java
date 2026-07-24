package com.aicoach.backend.api;

import com.aicoach.backend.models.Activity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/activities")
public interface ActivityApi {

    @PostMapping
    ResponseEntity<Activity> createActivity(@RequestBody Activity activity);
}
