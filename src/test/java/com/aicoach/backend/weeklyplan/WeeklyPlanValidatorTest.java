package com.aicoach.backend.weeklyplan;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.aicoach.backend.enums.DurationType;
import com.aicoach.backend.enums.WorkoutType;
import com.aicoach.backend.training.daniels.DanielsIntensity;
import com.aicoach.backend.training.workout.WorkoutStepDefinition;

import static org.junit.jupiter.api.Assertions.*;

class WeeklyPlanValidatorTest {
    private final WeeklyPlanValidator validator = new WeeklyPlanValidator();
    private final WeeklyPlanGenerationContext context = WeeklyPlanFixtures.context(LocalDate.of(2026, 1, 5));

    @Test
    void acceptsPlanThatMatchesAvailabilityRecoveryVolumeAndDanielsLimits() {
        WeeklyPlanCalculation result = validator.validate(context, WeeklyPlanFixtures.validProposal(context));

        assertEquals(15_000, result.totalDistanceMeters());
        assertEquals(2, result.sessions().size());
    }

    @Test
    void rejectsSessionOnUnavailableDayBeforePersistence() {
        var valid = WeeklyPlanFixtures.validProposal(context);
        var sessions = new ArrayList<>(valid.sessions());
        var first = sessions.get(0);
        sessions.set(0, new WeeklyPlanProposal.Session(first.order(), first.name(),
                context.weekStart().plusDays(2), first.workoutType(), first.blocks()));

        WeeklyPlanValidationException exception = assertThrows(WeeklyPlanValidationException.class,
                () -> validator.validate(context, new WeeklyPlanProposal(valid.summary(), sessions)));
        assertTrue(exception.getViolations().stream().anyMatch(value -> value.contains("indisponível")));
    }

    @Test
    void rejectsWeeklyVolumeOutsideTolerance() {
        var valid = WeeklyPlanFixtures.validProposal(context);
        var sessions = new ArrayList<>(valid.sessions());
        var first = sessions.get(0);
        var block = first.blocks().get(0);
        var step = block.steps().get(0);
        var shorter = new WeeklyPlanProposal.Step(step.kind(), step.durationType(), 5_000,
                step.intensity(), step.instruction());
        sessions.set(0, new WeeklyPlanProposal.Session(first.order(), first.name(), first.scheduledDate(),
                first.workoutType(), java.util.List.of(new WeeklyPlanProposal.Block(1, java.util.List.of(shorter)))));

        assertThrows(WeeklyPlanValidationException.class,
                () -> validator.validate(context, new WeeklyPlanProposal(valid.summary(), sessions)));
    }

    @Test
    void appliesActiveAdaptationToTheNextWeeklyVolume() {
        WeeklyPlanGenerationContext adapted = WeeklyPlanFixtures.withAdaptation(context, 25, false);
        WeeklyPlanProposal proposal = new WeeklyPlanProposal("Semana reduzida para recuperação", List.of(
                new WeeklyPlanProposal.Session(1, "Rodagem fácil", adapted.weekStart().plusDays(1),
                        WorkoutType.EASY_RUN, List.of(new WeeklyPlanProposal.Block(1, List.of(
                        new WeeklyPlanProposal.Step(WorkoutStepDefinition.Kind.WORK, DurationType.DISTANCE,
                                7_875, DanielsIntensity.E, "Leve"))))),
                new WeeklyPlanProposal.Session(2, "Longo reduzido", adapted.weekStart().plusDays(6),
                        WorkoutType.LONG_RUN, List.of(new WeeklyPlanProposal.Block(1, List.of(
                        new WeeklyPlanProposal.Step(WorkoutStepDefinition.Kind.WORK, DurationType.DISTANCE,
                                3_375, DanielsIntensity.E, "Leve")))))));

        WeeklyPlanCalculation result = validator.validate(adapted, proposal);

        assertEquals(11.25, adapted.effectiveTargetVolumeKm());
        assertEquals(11_250, result.totalDistanceMeters());
    }
}
