package com.aicoach.backend.api;

import com.aicoach.backend.dto.PlanningRunResponse;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/athletes/{athleteId}/planning-runs")
public interface PlanningRunApi {
    @PostMapping
    PlanningRunResponse synchronizeAndGenerate(@PathVariable Long athleteId);
}
