package com.aicoach.backend.dto;

import com.aicoach.backend.enums.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SecondaryRaceResponse(
        Long id, Long athleteId, Long globalPlanId, PrimaryTarget primaryTarget,
        String title, LocalDate raceDate, Integer distanceMeters, Integer targetTimeSeconds,
        RacePriority priority, SecondaryRaceStatus status, String ruleVersion,
        LocalDate windowStart, LocalDate windowEnd, String rationale, Instant createdAt,
        List<Adjustment> adjustments) {

    public record PrimaryTarget(Long objectiveId, String title, LocalDate raceDate,
                                Integer distanceMeters, RacePriority priority) {}

    public record Adjustment(Long weeklyPlanId, Long plannedActivityId,
                             RaceAdjustmentWindow window, SecondaryRaceAdjustmentAction action,
                             LocalDate originalDate, LocalDate proposedDate,
                             WorkoutType originalWorkoutType, WorkoutType proposedWorkoutType,
                             Integer originalDistanceMeters, Integer proposedDistanceMeters,
                             Integer originalDurationSeconds, Integer proposedDurationSeconds,
                             Integer loadPercent, String rationale) {}
}
