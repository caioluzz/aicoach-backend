package com.aicoach.backend.dto;

import com.aicoach.backend.enums.RacePriority;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record SecondaryRaceRequest(
        @NotNull Long globalPlanId,
        @NotBlank @Size(max = 200) String title,
        @NotNull LocalDate raceDate,
        @NotNull @Min(1000) @Max(100000) Integer distanceMeters,
        @Positive Integer targetTimeSeconds,
        @NotNull RacePriority priority) {
}
