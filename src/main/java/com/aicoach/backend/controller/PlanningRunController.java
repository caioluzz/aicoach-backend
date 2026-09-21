package com.aicoach.backend.controller;

import com.aicoach.backend.api.PlanningRunApi;
import com.aicoach.backend.dto.PlanningRunResponse;
import com.aicoach.backend.service.PlanningRunService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PlanningRunController implements PlanningRunApi {
    private final PlanningRunService service;

    @Override
    public PlanningRunResponse synchronizeAndGenerate(Long athleteId) {
        return service.synchronizeAndGenerate(athleteId);
    }
}
