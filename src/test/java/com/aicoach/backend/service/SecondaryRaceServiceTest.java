package com.aicoach.backend.service;

import com.aicoach.backend.dto.SecondaryRaceRequest;
import com.aicoach.backend.enums.*;
import com.aicoach.backend.models.*;
import com.aicoach.backend.repository.*;
import com.aicoach.backend.secondaryrace.SecondaryRaceAdjustmentEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SecondaryRaceServiceTest {
    private AthleteRepo athleteRepo;
    private GlobalPlanRepo globalPlanRepo;
    private SecondaryRaceRepo secondaryRaceRepo;
    private PlannedActivityRepo plannedActivityRepo;
    private SecondaryRaceService service;
    private GlobalPlan plan;

    @BeforeEach
    void setUp() {
        athleteRepo = mock(AthleteRepo.class);
        globalPlanRepo = mock(GlobalPlanRepo.class);
        secondaryRaceRepo = mock(SecondaryRaceRepo.class);
        plannedActivityRepo = mock(PlannedActivityRepo.class);
        service = new SecondaryRaceService(athleteRepo, globalPlanRepo, secondaryRaceRepo,
                plannedActivityRepo, new SecondaryRaceAdjustmentEngine(),
                Clock.fixed(Instant.parse("2026-09-21T12:00:00Z"), ZoneOffset.UTC));

        Athlete athlete = new Athlete();
        athlete.setId(7L);
        Objective primary = new Objective();
        primary.setId(3L);
        primary.setAthlete(athlete);
        primary.setTitle("Maratona principal");
        primary.setTargetDate(LocalDate.of(2026, 12, 6));
        primary.setTargetDistance_m(42195);
        primary.setPriority(RacePriority.A_RACE);
        plan = new GlobalPlan();
        plan.setId(9L);
        plan.setAthlete(athlete);
        plan.setObjective(primary);
        plan.setStatus(SeasonPlanStatus.APPROVED);
        plan.setStartDate(LocalDate.of(2026, 9, 1));
        plan.setEndDate(primary.getTargetDate());
        when(athleteRepo.findById(7L)).thenReturn(Optional.of(athlete));
        when(globalPlanRepo.findByIdAndAthleteId(9L, 7L)).thenReturn(Optional.of(plan));
        when(secondaryRaceRepo.save(any())).thenAnswer(invocation -> {
            SecondaryRace race = invocation.getArgument(0);
            race.setId(20L);
            return race;
        });
    }

    @Test
    void registersFiveKAndReturnsAuditableReorganizationWithoutChangingPrimaryTarget() {
        LocalDate raceDate = LocalDate.of(2026, 10, 4);
        WeeklyPlan raceWeek = weeklyPlan(30L);
        WeeklyPlan nextWeek = weeklyPlan(31L);
        PlannedActivity quality = activity(40L, raceWeek, raceDate.minusDays(3), WorkoutType.QUALITY_1);
        PlannedActivity recovery = activity(41L, nextWeek, raceDate.plusDays(1), WorkoutType.EASY_RUN);
        when(plannedActivityRepo.findSecondaryRaceWindow(eq(7L), eq(9L), any(), any(), any()))
                .thenReturn(List.of(quality, recovery));

        var response = service.create(7L, new SecondaryRaceRequest(9L, "5 km do bairro",
                raceDate, 5000, 1200, RacePriority.B_RACE));

        assertEquals(3L, response.primaryTarget().objectiveId());
        assertEquals(LocalDate.of(2026, 12, 6), response.primaryTarget().raceDate());
        assertEquals(SecondaryRaceAdjustmentAction.REPLACE_WITH_RACE,
                response.adjustments().get(0).action());
        assertTrue(response.adjustments().stream().anyMatch(item -> item.weeklyPlanId().equals(31L)
                && item.window() == RaceAdjustmentWindow.POST_RACE));
        assertEquals(RacePriority.A_RACE, plan.getObjective().getPriority());
        verify(secondaryRaceRepo).save(any(SecondaryRace.class));
    }

    @Test
    void rejectsRaceTooCloseToPrimaryTarget() {
        SecondaryRaceRequest request = new SecondaryRaceRequest(9L, "10 km",
                LocalDate.of(2026, 11, 30), 10000, null, RacePriority.B_RACE);

        assertThrows(SecondaryRaceValidationException.class, () -> service.create(7L, request));
        verifyNoInteractions(plannedActivityRepo);
    }

    @Test
    void rejectsPrimaryPriorityForSecondaryRace() {
        SecondaryRaceRequest request = new SecondaryRaceRequest(9L, "Outra principal",
                LocalDate.of(2026, 10, 4), 5000, null, RacePriority.A_RACE);

        assertThrows(SecondaryRaceValidationException.class, () -> service.create(7L, request));
    }

    private WeeklyPlan weeklyPlan(Long id) {
        WeeklyPlan weekly = new WeeklyPlan();
        weekly.setId(id);
        weekly.setAthlete(plan.getAthlete());
        weekly.setGlobalPlan(plan);
        weekly.setStatus(WeeklyPlanStatus.APPROVED);
        return weekly;
    }

    private PlannedActivity activity(Long id, WeeklyPlan weekly, LocalDate date, WorkoutType type) {
        PlannedActivity activity = new PlannedActivity();
        activity.setId(id);
        activity.setWeeklyPlan(weekly);
        activity.setScheduledDate(date);
        activity.setWorkoutType(type);
        activity.setPlannedDistanceMeters(7000);
        activity.setPlannedDurationSeconds(2700);
        return activity;
    }
}
