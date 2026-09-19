package com.aicoach.backend.dto;

import com.aicoach.backend.enums.*;

import java.time.*;
import java.util.List;
import java.util.Set;

public record AthleteAssessmentResponse(
        Long id,
        Long athleteId,
        Integer version,
        String schemaVersion,
        OnboardingStatus onboardingStatus,
        Instant createdAt,
        Instant completedAt,
        PhysicalProfile physicalProfile,
        RunningProfile runningProfile,
        List<Availability> availability,
        DayOfWeek preferredLongRunDay,
        Set<RunningSurface> surfaces,
        Set<TrainingEquipment> equipment,
        StrengthTraining strengthTraining,
        HealthProfile health,
        RecoveryProfile recovery,
        TargetRace targetRace
) {
    public record PhysicalProfile(LocalDate dateOfBirth, Double weightKg, Integer heightCm, Gender gender) {}
    public record RunningProfile(RunningExperienceLevel experienceLevel, Integer runningYears,
                                 Double currentWeeklyVolumeKm, Double recentAverageWeeklyVolumeKm,
                                 Double recentLongestRunKm, Integer currentRunsPerWeek) {}
    public record Availability(DayOfWeek dayOfWeek, Integer availableMinutes) {}
    public record StrengthTraining(Integer sessionsPerWeek, String notes) {}
    public record HealthProfile(boolean hasMedicalRestrictions, String medicalRestrictions,
                                List<HealthIssue> issues) {}
    public record HealthIssue(String bodyArea, String description, HealthIssueStatus status,
                              Integer painSeverity, LocalDate startedOn, String restrictionNotes) {}
    public record RecoveryProfile(Double averageSleepHours, SleepQuality sleepQuality,
                                  Integer recoveryDaysPerWeek, RoutineType routineType, String routineNotes) {}
    public record TargetRace(String title, LocalDate date, Integer distanceMeters,
                             Integer desiredTimeSeconds, RacePriority priority) {}
}
