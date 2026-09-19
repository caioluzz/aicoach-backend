package com.aicoach.backend.api;

import com.aicoach.backend.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/athletes/{athleteId}/season-plans")
public interface SeasonPlanApi {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SeasonPlanResponse create(@PathVariable Long athleteId, @Valid @RequestBody SeasonPlanCreateRequest request);

    @GetMapping("/{planId}")
    SeasonPlanResponse get(@PathVariable Long athleteId, @PathVariable Long planId);

    @GetMapping("/latest")
    SeasonPlanResponse getLatest(@PathVariable Long athleteId);

    @GetMapping
    List<SeasonPlanResponse> getHistory(@PathVariable Long athleteId);

    @PostMapping("/{planId}/review")
    SeasonPlanResponse review(@PathVariable Long athleteId, @PathVariable Long planId,
                              @Valid @RequestBody SeasonPlanReviewRequest request);
}
