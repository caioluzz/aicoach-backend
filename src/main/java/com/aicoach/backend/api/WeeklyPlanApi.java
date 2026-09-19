package com.aicoach.backend.api;

import com.aicoach.backend.dto.WeeklyPlanCreateRequest;
import com.aicoach.backend.dto.WeeklyPlanResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/athletes/{athleteId}/weekly-plans")
public interface WeeklyPlanApi {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    WeeklyPlanResponse create(@PathVariable Long athleteId, @Valid @RequestBody WeeklyPlanCreateRequest request);

    @GetMapping("/{weeklyPlanId}")
    WeeklyPlanResponse get(@PathVariable Long athleteId, @PathVariable Long weeklyPlanId);

    @GetMapping("/latest")
    WeeklyPlanResponse getLatest(@PathVariable Long athleteId, @RequestParam Long seasonPlanId,
                                 @RequestParam Integer weekNumber);

    @GetMapping
    List<WeeklyPlanResponse> getHistory(@PathVariable Long athleteId, @RequestParam Long seasonPlanId,
                                        @RequestParam Integer weekNumber);
}
