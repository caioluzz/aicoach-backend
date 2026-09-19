package com.aicoach.backend.dto;

import jakarta.validation.constraints.NotNull;

public record SeasonPlanCreateRequest(
        @NotNull(message = "A prova-alvo é obrigatória") Long objectiveId
) {}
