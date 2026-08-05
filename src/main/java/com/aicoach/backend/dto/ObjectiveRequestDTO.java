package com.aicoach.backend.dto;

import com.aicoach.backend.enums.RacePriority;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record ObjectiveRequestDTO(
        @NotBlank(message = "O título da prova é obrigatório")
        String title,

        @NotNull(message = "A distância é obrigatória")
        @Positive(message = "A distância deve ser maior que zero")
        Integer distanceMeters,

        @NotNull(message = "A data alvo é obrigatória")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate targetDate,

        @NotBlank(message = "A prioridade da prova é obrigatória")
        String priority,

        @NotBlank(message = "O status é obrigatório")
        String status
) {}