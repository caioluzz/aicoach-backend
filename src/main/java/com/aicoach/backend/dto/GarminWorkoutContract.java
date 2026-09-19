package com.aicoach.backend.dto;

import com.aicoach.backend.enums.DurationType;
import com.aicoach.backend.enums.IntensityZone;
import com.aicoach.backend.enums.StepType;

import java.time.LocalDate;
import java.util.List;

public record GarminWorkoutContract(
        String schemaVersion,
        String name,
        LocalDate scheduledDate,
        List<Block> blocks) {
    public static final String SCHEMA_VERSION = "workout.v1";

    public record Block(Integer order, Integer repetitions, List<Step> steps) {}
    public record Step(
            Integer order,
            StepType kind,
            DurationType durationType,
            Integer durationValue,
            IntensityZone targetZone,
            Integer targetPaceFastestSecondsPerKm,
            Integer targetPaceSlowestSecondsPerKm,
            String instruction) {}
}
