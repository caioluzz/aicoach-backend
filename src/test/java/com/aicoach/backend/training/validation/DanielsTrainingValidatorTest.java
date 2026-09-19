package com.aicoach.backend.training.validation;

import com.aicoach.backend.training.daniels.DanielsIntensity;
import com.aicoach.backend.training.validation.TrainingLoad.SessionLoad;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DanielsTrainingValidatorTest {

    private final DanielsTrainingValidator validator = new DanielsTrainingValidator();

    @Test
    void acceptsProgressionDistributionRecoveryAndIntensityCaps() {
        TrainingLoad load = new TrainingLoad(
                53_000, 18_000, 50_000, 17_000, 3, 5,
                List.of(
                        session(1, DanielsIntensity.E, 12_000, 4_200),
                        session(2, DanielsIntensity.I, 4_000, 1_200),
                        session(3, DanielsIntensity.E, 10_000, 3_600),
                        session(4, DanielsIntensity.E, 10_000, 3_400),
                        session(5, DanielsIntensity.T, 5_000, 1_700),
                        session(6, DanielsIntensity.E, 12_000, 3_900)));

        assertThat(validator.validateWeek(load)).isEmpty();
    }

    @Test
    void reportsAllDeterministicWeeklyViolations() {
        TrainingLoad load = new TrainingLoad(
                70_000, 25_000, 50_000, 18_000, 1, 5,
                List.of(
                        session(1, DanielsIntensity.E, 10_000, 3_000),
                        session(2, DanielsIntensity.I, 8_000, 2_000),
                        session(3, DanielsIntensity.T, 8_000, 3_000),
                        session(4, DanielsIntensity.R, 5_000, 1_000)));

        assertThat(validator.validateWeek(load))
                .extracting(ValidationIssue::code)
                .contains(
                        "PROGRESSION_STABILITY",
                        "PROGRESSION_DISTANCE",
                        "PROGRESSION_DURATION",
                        "INTENSITY_DISTRIBUTION",
                        "QUALITY_RECOVERY",
                        "THRESHOLD_LIMIT",
                        "INTERVAL_LIMIT",
                        "REPETITION_LIMIT");
    }

    private static SessionLoad session(int day, DanielsIntensity intensity, int distance, int duration) {
        return new SessionLoad(LocalDate.of(2026, 9, day), intensity, distance, duration);
    }
}
