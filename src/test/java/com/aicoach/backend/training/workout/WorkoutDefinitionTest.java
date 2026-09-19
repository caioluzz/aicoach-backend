package com.aicoach.backend.training.workout;

import com.aicoach.backend.training.daniels.DanielsIntensity;
import com.aicoach.backend.training.daniels.DanielsPerformanceEngine;
import com.aicoach.backend.training.daniels.PaceRange;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkoutDefinitionTest {

    @Test
    void representsVersionedProviderNeutralWorkout() {
        WorkoutStepDefinition work = new WorkoutStepDefinition(
                WorkoutStepDefinition.Kind.WORK,
                new WorkoutDuration(WorkoutDuration.Unit.METERS, 1_000),
                DanielsIntensity.I,
                new PaceRange(230, 240),
                "Controlled I pace");
        WorkoutStepDefinition recovery = new WorkoutStepDefinition(
                WorkoutStepDefinition.Kind.RECOVERY,
                new WorkoutDuration(WorkoutDuration.Unit.SECONDS, 180),
                DanielsIntensity.E,
                null,
                "Easy jog");

        WorkoutDefinition workout = new WorkoutDefinition(
                WorkoutDefinition.CURRENT_SCHEMA_VERSION,
                DanielsPerformanceEngine.ENGINE_VERSION,
                "5 x 1 km I",
                LocalDate.of(2026, 9, 22),
                List.of(new WorkoutBlockDefinition(5, List.of(work, recovery))));

        assertThat(workout.schemaVersion()).isEqualTo("workout.v1");
        assertThat(workout.blocks().get(0).repetitions()).isEqualTo(5);
        assertThat(workout.toString()).doesNotContain("garmin");
    }

    @Test
    void rejectsUnknownSchemaVersion() {
        assertThatThrownBy(() -> new WorkoutDefinition(
                "workout.v2", "engine", "name", LocalDate.now(),
                List.of(new WorkoutBlockDefinition(1, List.of(
                        new WorkoutStepDefinition(WorkoutStepDefinition.Kind.WORK,
                                new WorkoutDuration(WorkoutDuration.Unit.SECONDS, 60),
                                DanielsIntensity.E, null, null))))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported workout schema");
    }
}
