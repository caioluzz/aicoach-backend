package com.aicoach.backend.training.validation;

import com.aicoach.backend.training.daniels.DanielsIntensity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DanielsStimulusRulesTest {

    private final DanielsStimulusRules rules = new DanielsStimulusRules();

    @Test
    void acceptsCanonicalIntervalRepetition() {
        assertThat(rules.validateRepetition(DanielsIntensity.I, 240, 180, DanielsIntensity.E)).isEmpty();
    }

    @Test
    void rejectsLongIntervalAndExcessRecovery() {
        assertThat(rules.validateRepetition(DanielsIntensity.I, 301, 302, DanielsIntensity.E))
                .extracting(ValidationIssue::code)
                .containsExactly("I_WORK_DURATION", "I_RECOVERY_DURATION");
    }

    @Test
    void validatesThresholdCruiseRecoveryRatio() {
        assertThat(rules.validateRepetition(DanielsIntensity.T, 600, 120, DanielsIntensity.E)).isEmpty();
        assertThat(rules.validateRepetition(DanielsIntensity.T, 600, 90, DanielsIntensity.E))
                .extracting(ValidationIssue::code)
                .containsExactly("T_RECOVERY_DURATION");
    }

    @Test
    void validatesRepetitionRecoveryAndContinuousLimits() {
        assertThat(rules.validateRepetition(DanielsIntensity.R, 60, 180, DanielsIntensity.E)).isEmpty();
        assertThat(rules.validateRepetition(DanielsIntensity.R, 121, 121, DanielsIntensity.E))
                .extracting(ValidationIssue::code)
                .containsExactly("R_WORK_DURATION", "R_RECOVERY_DURATION");
        assertThat(rules.validateContinuous(DanielsIntensity.T, 1_200, 4_000, 14_400, 50_000)).isEmpty();
    }
}
