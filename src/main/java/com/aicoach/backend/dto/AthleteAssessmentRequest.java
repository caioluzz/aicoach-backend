package com.aicoach.backend.dto;

import com.aicoach.backend.enums.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record AthleteAssessmentRequest(
        @NotNull OnboardingStatus onboardingStatus,
        @Valid @NotNull PhysicalProfile physicalProfile,
        @Valid @NotNull RunningProfile runningProfile,
        @NotEmpty List<@Valid Availability> availability,
        @NotNull DayOfWeek preferredLongRunDay,
        @NotEmpty Set<RunningSurface> surfaces,
        @NotNull Set<TrainingEquipment> equipment,
        @Valid @NotNull StrengthTraining strengthTraining,
        @Valid @NotNull HealthProfile health,
        @Valid @NotNull RecoveryProfile recovery,
        @Valid @NotNull TargetRace targetRace
) {
    public record PhysicalProfile(
            @NotNull @Past @JsonFormat(pattern = "yyyy-MM-dd") LocalDate dateOfBirth,
            @NotNull @DecimalMin("25.0") @DecimalMax("350.0") Double weightKg,
            @NotNull @Min(100) @Max(250) Integer heightCm,
            @NotNull Gender gender
    ) {}

    public record RunningProfile(
            @NotNull RunningExperienceLevel experienceLevel,
            @NotNull @Min(0) @Max(80) Integer runningYears,
            @NotNull @DecimalMin("0.0") @DecimalMax("400.0") Double currentWeeklyVolumeKm,
            @NotNull @DecimalMin("0.0") @DecimalMax("400.0") Double recentAverageWeeklyVolumeKm,
            @NotNull @DecimalMin("0.0") @DecimalMax("200.0") Double recentLongestRunKm,
            @NotNull @Min(0) @Max(14) Integer currentRunsPerWeek
    ) {}

    public record Availability(
            @NotNull DayOfWeek dayOfWeek,
            @NotNull @Min(15) @Max(1440) Integer availableMinutes
    ) {}

    public record StrengthTraining(
            @NotNull @Min(0) @Max(14) Integer sessionsPerWeek,
            @Size(max = 500) String notes
    ) {}

    public record HealthProfile(
            boolean hasMedicalRestrictions,
            @Size(max = 1000) String medicalRestrictions,
            @NotNull List<@Valid HealthIssue> issues
    ) {}

    public record HealthIssue(
            @NotBlank @Size(max = 100) String bodyArea,
            @NotBlank @Size(max = 500) String description,
            @NotNull HealthIssueStatus status,
            @NotNull @Min(0) @Max(10) Integer painSeverity,
            @PastOrPresent @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startedOn,
            @Size(max = 500) String restrictionNotes
    ) {}

    public record RecoveryProfile(
            @NotNull @DecimalMin("0.0") @DecimalMax("24.0") Double averageSleepHours,
            @NotNull SleepQuality sleepQuality,
            @NotNull @Min(0) @Max(7) Integer recoveryDaysPerWeek,
            @NotNull RoutineType routineType,
            @Size(max = 500) String routineNotes
    ) {}

    public record TargetRace(
            @NotBlank @Size(max = 255) String title,
            @NotNull @Future @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @NotNull @Positive @Max(500000) Integer distanceMeters,
            @NotNull @Positive Integer desiredTimeSeconds,
            @NotNull RacePriority priority
    ) {}
}
