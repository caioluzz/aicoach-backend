package com.aicoach.backend.dto;

import com.aicoach.backend.enums.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record WeeklyPlanResponse(
        Long id,
        Long athleteId,
        Long seasonPlanId,
        Integer seasonPlanWeekNumber,
        Integer version,
        WeeklyPlanStatus status,
        Long sourceWeeklyPlanId,
        LocalDate weekStart,
        LocalDate weekEnd,
        Double targetVolumeKm,
        Integer plannedDistanceMeters,
        Integer plannedDurationSeconds,
        String summary,
        Instant createdAt,
        Instant reviewedAt,
        String reviewComment,
        Generation generation,
        Validation validation,
        List<Session> sessions) {

    public record Generation(String source, String responseId, String model, String promptVersion,
                             String schemaVersion, Integer inputTokens, Integer outputTokens, Long latencyMs) {}

    public record Validation(boolean valid, Instant validatedAt, String validatorVersion,
                             List<Message> messages) {}

    public record Message(ValidationSeverity severity, String code, String message) {}

    public record Session(Integer order, String name, LocalDate scheduledDate, WorkoutType workoutType,
                          Integer plannedDistanceMeters, Integer plannedDurationSeconds, List<Block> blocks) {}

    public record Block(Integer order, Integer repetitions, List<Step> steps) {}

    public record Step(Integer order, StepType kind, DurationType durationType, Integer durationValue,
                       IntensityZone targetZone, Integer targetPaceFastestSecondsPerKm,
                       Integer targetPaceSlowestSecondsPerKm, String instruction) {}
}
