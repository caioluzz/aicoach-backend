package com.aicoach.backend.controller;

import com.aicoach.backend.api.VdotTestApi;
import com.aicoach.backend.dto.VdotTestWorkoutResponse;
import com.aicoach.backend.service.VdotTestWorkoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class VdotTestController implements VdotTestApi {
    private final VdotTestWorkoutService service;

    @Override
    public VdotTestWorkoutResponse preview(Long athleteId) {
        return service.preview(athleteId);
    }

    @Override
    public VdotTestWorkoutResponse deliver(Long athleteId) {
        return service.deliver(athleteId);
    }
}
