package com.aicoach.backend.secondaryrace;

import com.aicoach.backend.enums.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecondaryRaceAdjustmentEngineTest {
    private final SecondaryRaceAdjustmentEngine engine = new SecondaryRaceAdjustmentEngine();

    @Test
    void fiveKilometerBRaceReplacesQualityAndProtectsAdjacentWeek() {
        LocalDate raceDate = LocalDate.of(2026, 10, 4);
        var decision = engine.evaluate(new SecondaryRaceAdjustmentEngine.Input(
                raceDate, 5000, 1200, RacePriority.B_RACE, List.of(
                session(1L, 10L, raceDate.minusDays(3), WorkoutType.QUALITY_1, 7000),
                session(2L, 10L, raceDate.minusDays(1), WorkoutType.EASY_RUN, 5000),
                session(3L, 11L, raceDate.plusDays(1), WorkoutType.QUALITY_2, 8000),
                session(4L, 11L, raceDate.plusDays(3), WorkoutType.EASY_RUN, 6000))));

        assertEquals(raceDate.minusDays(5), decision.windowStart());
        assertEquals(raceDate.plusDays(4), decision.windowEnd());
        var race = decision.adjustments().stream()
                .filter(item -> item.action() == SecondaryRaceAdjustmentAction.REPLACE_WITH_RACE)
                .findFirst().orElseThrow();
        assertEquals(1L, race.sessionId());
        assertEquals(WorkoutType.RACE, race.proposedType());
        assertEquals(5000, race.proposedDistanceMeters());
        assertTrue(decision.adjustments().stream().anyMatch(item -> item.sessionId().equals(2L)
                && item.action() == SecondaryRaceAdjustmentAction.REST));
        assertTrue(decision.adjustments().stream().anyMatch(item -> item.sessionId().equals(3L)
                && item.action() == SecondaryRaceAdjustmentAction.REST));
        assertTrue(decision.adjustments().stream().anyMatch(item -> item.sessionId().equals(4L)
                && item.action() == SecondaryRaceAdjustmentAction.RECOVERY));
    }

    @Test
    void addsRaceWhenThereIsNoQualitySession() {
        LocalDate raceDate = LocalDate.of(2026, 10, 4);
        var decision = engine.evaluate(new SecondaryRaceAdjustmentEngine.Input(
                raceDate, 5000, null, RacePriority.C_RACE, List.of()));

        assertEquals(SecondaryRaceAdjustmentAction.ADD_RACE,
                decision.adjustments().get(0).action());
        assertNull(decision.adjustments().get(0).sessionId());
        assertEquals(WorkoutType.RACE, decision.adjustments().get(0).proposedType());
    }

    @Test
    void rejectsAnotherPrimaryRace() {
        assertThrows(IllegalArgumentException.class, () -> engine.evaluate(
                new SecondaryRaceAdjustmentEngine.Input(LocalDate.of(2026, 10, 4), 5000,
                        null, RacePriority.A_RACE, List.of())));
    }

    private SecondaryRaceAdjustmentEngine.Session session(Long id, Long planId, LocalDate date,
                                                           WorkoutType type, int distance) {
        return new SecondaryRaceAdjustmentEngine.Session(id, planId, date, type, distance, 2400);
    }
}
