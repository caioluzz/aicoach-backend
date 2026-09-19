package com.aicoach.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WeeklyPlanCreateRequest(
        @NotNull Long seasonPlanId,
        @NotNull @Min(1) Integer weekNumber) {
}
