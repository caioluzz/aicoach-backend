package com.aicoach.backend.dto;

import com.aicoach.backend.enums.SeasonPlanReviewDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SeasonPlanReviewRequest(
        @NotNull(message = "A decisão é obrigatória") SeasonPlanReviewDecision decision,
        @Size(max = 1000, message = "O comentário deve ter no máximo 1000 caracteres") String comment
) {}
