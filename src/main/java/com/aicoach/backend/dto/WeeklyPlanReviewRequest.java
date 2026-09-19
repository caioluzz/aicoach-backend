package com.aicoach.backend.dto;

import com.aicoach.backend.enums.WeeklyPlanReviewDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WeeklyPlanReviewRequest(
        @NotNull(message = "A decisão é obrigatória") WeeklyPlanReviewDecision decision,
        @Size(max = 1000, message = "O comentário deve ter no máximo 1000 caracteres") String comment
) {}
