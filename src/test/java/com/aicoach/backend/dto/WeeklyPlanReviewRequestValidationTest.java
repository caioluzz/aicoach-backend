package com.aicoach.backend.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class WeeklyPlanReviewRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void requiresDecisionAndReasonsForReplacementVersions() {
        assertFalse(validator.validate(new WeeklyPlanReviewRequest(null, null)).isEmpty());
        assertFalse(validator.validate(new WeeklyPlanRegenerateRequest(" ")).isEmpty());
        assertFalse(validator.validate(new WeeklyPlanEditRequest("Resumo", java.util.List.of(), " ")).isEmpty());
    }
}
