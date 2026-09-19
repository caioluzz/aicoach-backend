package com.aicoach.backend.weeklyplan;

import com.aicoach.backend.enums.*;
import com.aicoach.backend.training.daniels.DanielsIntensity;
import com.aicoach.backend.training.workout.WorkoutStepDefinition;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public final class WeeklyPlanFixtures {
    private WeeklyPlanFixtures() {}

    public static WeeklyPlanGenerationContext context(LocalDate monday) {
        return new WeeklyPlanGenerationContext(1L, 10L, 100L, 1, monday, monday.plusDays(6),
                15.0, "Consolidar base", false, false, TrainingPhase.BASE, "Base aeróbia", 15.0,
                monday.plusMonths(3), 10_000, 1,
                List.of(new WeeklyPlanGenerationContext.Availability(DayOfWeek.TUESDAY, 60),
                        new WeeklyPlanGenerationContext.Availability(DayOfWeek.SUNDAY, 120)),
                DayOfWeek.SUNDAY, 4, 2, false, null, List.of(), 7.5, SleepQuality.GOOD,
                RoutineType.FIXED, "Trabalho diurno", 45.0,
                new WeeklyPlanGenerationContext.PaceProfile(330, 300, 280, 255, 235));
    }

    public static WeeklyPlanProposal validProposal(WeeklyPlanGenerationContext context) {
        return new WeeklyPlanProposal("Semana de base com duas rodagens controladas", List.of(
                new WeeklyPlanProposal.Session(1, "Rodagem fácil", context.weekStart().plusDays(1),
                        WorkoutType.EASY_RUN, List.of(new WeeklyPlanProposal.Block(1, List.of(
                        step(WorkoutStepDefinition.Kind.WORK, DurationType.DISTANCE, 10_500,
                                DanielsIntensity.E, "Ritmo fácil e confortável"))))),
                new WeeklyPlanProposal.Session(2, "Longo fácil", context.weekStart().plusDays(6),
                        WorkoutType.LONG_RUN, List.of(new WeeklyPlanProposal.Block(1, List.of(
                        step(WorkoutStepDefinition.Kind.WORK, DurationType.DISTANCE, 4_500,
                                DanielsIntensity.E, "Mantenha conversa confortável")))))));
    }

    private static WeeklyPlanProposal.Step step(WorkoutStepDefinition.Kind kind, DurationType durationType,
                                                 int value, DanielsIntensity intensity, String instruction) {
        return new WeeklyPlanProposal.Step(kind, durationType, value, intensity, instruction);
    }
}
