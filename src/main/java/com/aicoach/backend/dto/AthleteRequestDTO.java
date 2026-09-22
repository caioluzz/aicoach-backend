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

        @Email(message = "Formato de email inválido")
        String garminEmail,

        String garminPassword,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dateOfBirth,

        @Positive(message = "O peso deve ser maior que zero")
        Double weightKg,

        @Positive(message = "A altura deve ser maior que zero")
        Integer heightCm,

        Gender gender,

        Set<DayOfWeek> availableTrainingDays,

        List<@Valid ObjectiveRequestDTO> objectives
) {}
