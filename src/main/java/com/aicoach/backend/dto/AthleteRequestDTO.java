package com.aicoach.backend.dto;

import com.aicoach.backend.enums.Gender;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record AthleteRequestDTO(
        @NotBlank(message = "O nome é obrigatório")
        String name,

        @NotBlank(message = "O email do Garmin é obrigatório")
        @Email(message = "Formato de email inválido")
        String garminEmail,

        @NotBlank(message = "A senha do Garmin é obrigatória")
        String garminPassword,

        @NotNull(message = "A data de nascimento é obrigatória")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dateOfBirth,

        @NotNull(message = "O peso é obrigatório")
        @Positive(message = "O peso deve ser maior que zero")
        Double weightKg,

        @NotNull(message = "A altura é obrigatória")
        @Positive(message = "A altura deve ser maior que zero")
        Integer heightCm,

        @NotBlank(message = "O gênero é obrigatório")
        Gender gender,

        @NotEmpty(message = "É necessário informar pelo menos um dia de treino na semana")
        Set<DayOfWeek> availableTrainingDays,

        @Valid
        @NotEmpty(message = "É necessário cadastrar pelo menos um objetivo (prova alvo)")
        List<ObjectiveRequestDTO> objectives
) {}