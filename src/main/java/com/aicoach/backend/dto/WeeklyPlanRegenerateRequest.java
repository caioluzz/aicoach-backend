package com.aicoach.backend.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public record WeeklyPlanRegenerateRequest(
        @NotBlank(message = "O motivo é obrigatório")
        @Size(max = 1000, message = "O motivo deve ter no máximo 1000 caracteres") String reason
) {}
