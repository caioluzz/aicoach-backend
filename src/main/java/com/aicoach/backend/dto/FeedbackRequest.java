package com.aicoach.backend.dto;

import com.aicoach.backend.enums.FeedbackFeeling;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record FeedbackRequest(
        Long activityId,
        @NotNull LocalDate feedbackDate,
        @NotNull @Min(1) @Max(10) Integer perceivedEffort,
        @NotNull @Min(1) @Max(10) Integer fatigue,
        @NotNull @DecimalMin("0.0") @DecimalMax("24.0") Double sleepHours,
        @NotNull @Min(0) @Max(10) Integer painSeverity,
        @Size(max = 200) String painLocation,
        @NotNull FeedbackFeeling feeling,
        @Size(max = 2000) String comment) {
}
