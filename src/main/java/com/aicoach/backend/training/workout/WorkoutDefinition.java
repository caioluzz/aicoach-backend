package com.aicoach.backend.training.workout;

import java.time.LocalDate;
import java.util.List;

/** Provider-neutral workout contract. Garmin-specific fields do not belong here. */
public record WorkoutDefinition(
        String schemaVersion,
        String engineVersion,
        String name,
        LocalDate scheduledDate,
        List<WorkoutBlockDefinition> blocks) {

    public static final String CURRENT_SCHEMA_VERSION = "workout.v1";

    public WorkoutDefinition {
        if (!CURRENT_SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("Unsupported workout schema version: " + schemaVersion);
        }
        if (engineVersion == null || engineVersion.isBlank() || name == null || name.isBlank()
                || scheduledDate == null || blocks == null || blocks.isEmpty()) {
            throw new IllegalArgumentException("Workout definition is incomplete");
        }
        blocks = List.copyOf(blocks);
    }
}
