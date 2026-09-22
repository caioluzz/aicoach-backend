package com.aicoach.backend.dto;

import java.time.LocalDate;

public record AthleteSummaryResponse(
        Long id,
        String name,
        String garminEmail,
        LocalDate dateOfBirth
) {
}
