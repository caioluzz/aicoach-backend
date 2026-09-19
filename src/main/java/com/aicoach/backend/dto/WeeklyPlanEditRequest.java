package com.aicoach.backend.dto;

import com.aicoach.backend.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public record WeeklyPlanEditRequest(
        @NotBlank(message = "O resumo é obrigatório")
        @Size(max = 2000, message = "O resumo deve ter no máximo 2000 caracteres") String summary,
        @NotEmpty(message = "Informe ao menos uma sessão") List<@Valid Session> sessions,
        @NotBlank(message = "O motivo é obrigatório")
        @Size(max = 1000, message = "O motivo deve ter no máximo 1000 caracteres") String reason
) {
    public record Session(
            @NotNull(message = "A ordem da sessão é obrigatória") @Positive Integer order,
            @NotBlank(message = "O nome da sessão é obrigatório") @Size(max = 200) String name,
            @NotNull(message = "A data da sessão é obrigatória") LocalDate scheduledDate,
            @NotNull(message = "O tipo do treino é obrigatório") WorkoutType workoutType,
            @NotEmpty(message = "Informe ao menos um bloco") List<@Valid Block> blocks
    ) {}

    public record Block(
            @NotNull(message = "As repetições são obrigatórias") @Min(1) @Max(100) Integer repetitions,
            @NotEmpty(message = "Informe ao menos um passo") List<@Valid Step> steps
    ) {}

    public record Step(
            @NotNull(message = "O tipo do passo é obrigatório") StepType kind,
            @NotNull(message = "O tipo de duração é obrigatório") DurationType durationType,
            @NotNull(message = "A duração é obrigatória") @Positive Integer durationValue,
            @NotNull(message = "A zona-alvo é obrigatória") IntensityZone targetZone,
            @NotBlank(message = "A instrução é obrigatória") @Size(max = 500) String instruction
    ) {}
}
