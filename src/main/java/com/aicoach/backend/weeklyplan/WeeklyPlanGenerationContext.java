package com.aicoach.backend.weeklyplan;

import com.aicoach.backend.enums.*;
import com.aicoach.backend.training.daniels.DanielsIntensity;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public record WeeklyPlanGenerationContext(
        Long athleteId,
        Long seasonPlanId,
        Long seasonPlanWeekId,
        Integer weekNumber,
        LocalDate weekStart,
        LocalDate weekEnd,
        Double targetVolumeKm,
        String weekFocus,
        Boolean recoveryWeek,
        Boolean taperWeek,
        TrainingPhase phase,
        String phaseObjective,
        Double priorWeekTargetVolumeKm,
        LocalDate targetRaceDate,
        Integer targetRaceDistanceMeters,
        Integer assessmentVersion,
        List<Availability> availability,
        DayOfWeek preferredLongRunDay,
        Integer currentRunsPerWeek,
        Integer recoveryDaysPerWeek,
        Boolean hasMedicalRestrictions,
        String medicalRestrictions,
        List<HealthIssue> healthIssues,
        Double averageSleepHours,
        SleepQuality sleepQuality,
        RoutineType routineType,
        String routineNotes,
        Double vdot,
        PaceProfile paces) {

    public record Availability(DayOfWeek dayOfWeek, Integer availableMinutes) {}

    public record HealthIssue(String bodyArea, HealthIssueStatus status, Integer painSeverity,
                              String restrictionNotes) {}

    public record PaceProfile(Integer easy, Integer marathon, Integer threshold,
                              Integer interval, Integer repetition) {
        public int forIntensity(DanielsIntensity intensity) {
            Integer pace = switch (intensity) {
                case E -> easy;
                case M -> marathon;
                case T -> threshold;
                case I -> interval;
                case R -> repetition;
            };
            if (pace == null || pace <= 0) throw new IllegalArgumentException("Perfil Daniels incompleto");
            return pace;
        }
    }
}
