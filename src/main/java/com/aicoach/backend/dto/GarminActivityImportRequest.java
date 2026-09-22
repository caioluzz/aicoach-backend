package com.aicoach.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GarminActivityImportRequest(
        @NotNull @Positive Long garminActivityId
) {}
