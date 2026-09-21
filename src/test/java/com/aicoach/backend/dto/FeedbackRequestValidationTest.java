package com.aicoach.backend.dto;

import com.aicoach.backend.enums.FeedbackFeeling;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeedbackRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void enforcesClinicalScalesAndSleepRange() {
        FeedbackRequest invalid = new FeedbackRequest(null, LocalDate.of(2026, 9, 21),
                11, 0, 25.0, -1, null, FeedbackFeeling.NEUTRAL, null);

        assertEquals(4, validator.validate(invalid).size());
    }
}
