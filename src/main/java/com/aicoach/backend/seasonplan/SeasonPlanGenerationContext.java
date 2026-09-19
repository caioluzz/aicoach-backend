package com.aicoach.backend.seasonplan;

import com.aicoach.backend.enums.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public record SeasonPlanGenerationContext(
        Long athleteId,
        Integer assessmentVersion,
        Long performanceProfileId,
        LocalDate planStartDate,
        LocalDate raceDate,
        Integer totalWeeks,
        String raceTitle,
        Integer raceDistanceMeters,
        Integer targetTimeSeconds,
        RacePriority racePriority,
        RunningExperienceLevel experienceLevel,
        Double currentWeeklyVolumeKm,
        Double recentAverageWeeklyVolumeKm,
        Double recentLongestRunKm,
        Integer currentRunsPerWeek,
        List<Availability> availability,
        DayOfWeek preferredLongRunDay,
        Integer recoveryDaysPerWeek,
        Boolean hasMedicalRestrictions,
        List<HealthIssue> healthIssues,
        Double vdot,
        PaceProfile paces
) {
    public record Availability(DayOfWeek dayOfWeek, Integer availableMinutes) {}
    public record HealthIssue(String bodyArea, HealthIssueStatus status, Integer painSeverity,
                              String restrictionNotes) {}
    public record PaceProfile(Integer easySecondsPerKm, Integer marathonSecondsPerKm,
                              Integer thresholdSecondsPerKm, Integer intervalSecondsPerKm,
                              Integer repetitionSecondsPerKm) {}
}
