package com.aicoach.backend.service;

import com.aicoach.backend.dto.WeeklyPlanCreateRequest;
import com.aicoach.backend.dto.WeeklyPlanResponse;
import com.aicoach.backend.enums.*;
import com.aicoach.backend.models.*;
import com.aicoach.backend.repository.*;
import com.aicoach.backend.weeklyplan.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
class WeeklyPlanPersistenceTest {
    @Autowired AthleteRepo athleteRepo;
    @Autowired ObjectiveRepo objectiveRepo;
    @Autowired AthleteAssessmentRepo assessmentRepo;
    @Autowired AthleteMetricsRepo metricsRepo;
    @Autowired GlobalPlanRepo seasonPlanRepo;
    @Autowired WeeklyPlanRepo weeklyPlanRepo;
    @Autowired AthleteAssessmentService assessmentService;
    @Autowired WeeklyPlanService weeklyPlanService;
    @MockitoBean WeeklyPlanGenerator generator;

    private Athlete athlete;
    private GlobalPlan seasonPlan;

    @BeforeEach
    void prepareApprovedSeasonPlan() {
        athlete = new Athlete();
        athlete.setName("Runner");
        athlete = athleteRepo.saveAndFlush(athlete);
        assessmentService.createVersion(athlete.getId(),
                AthleteAssessmentServiceTest.validRequest(DayOfWeek.SUNDAY));
        AthleteAssessment assessment = assessmentRepo.findTopByAthleteIdOrderByVersionDesc(athlete.getId())
                .orElseThrow();

        Objective objective = new Objective();
        objective.setAthlete(athlete);
        objective.setTitle("10K");
        objective.setTargetDate(assessment.getTargetRaceDate());
        objective.setTargetDistance_m(assessment.getTargetRaceDistanceMeters());
        objective.setTargetTimeMinutes(assessment.getTargetRaceTimeSeconds() / 60);
        objective.setPriority(RacePriority.A_RACE);
        objective.setStatus(ObjectiveStatus.ACTIVE);
        objective = objectiveRepo.saveAndFlush(objective);

        AthleteMetrics metrics = new AthleteMetrics();
        metrics.setAthlete(athlete);
        metrics.setRecordedAt(LocalDateTime.now());
        metrics.setVdot(45.0);
        metrics.setEasyPaceSec(330);
        metrics.setMarathonPaceSec(300);
        metrics.setThresholdPaceSec(280);
        metrics.setIntervalPaceSec(255);
        metrics.setRepetitionPaceSec(235);
        metrics = metricsRepo.saveAndFlush(metrics);

        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        seasonPlan = new GlobalPlan();
        seasonPlan.setAthlete(athlete);
        seasonPlan.setObjective(objective);
        seasonPlan.setAssessment(assessment);
        seasonPlan.setAthleteMetrics(metrics);
        seasonPlan.setVersion(1);
        seasonPlan.setStatus(SeasonPlanStatus.APPROVED);
        seasonPlan.setStartDate(weekStart);
        seasonPlan.setEndDate(objective.getTargetDate());
        seasonPlan.setTotalWeeks(1);
        seasonPlan.setSummary("Plano aprovado");
        seasonPlan.setCreatedAt(Instant.now());
        seasonPlan.setReviewedAt(Instant.now());
        seasonPlan.setGenerationSource("TEST");
        seasonPlan.setModel("test");

        SeasonPlanWeek week = new SeasonPlanWeek();
        week.setGlobalPlan(seasonPlan);
        week.setWeekNumber(1);
        week.setStartDate(weekStart);
        week.setEndDate(weekStart.plusDays(6));
        week.setTargetVolumeKm(15.0);
        week.setFocus("Base");
        week.setRecoveryWeek(false);
        week.setTaperWeek(false);
        seasonPlan.setWeeks(new ArrayList<>(java.util.List.of(week)));

        TrainingCycle cycle = new TrainingCycle();
        cycle.setGlobalPlan(seasonPlan);
        cycle.setPhase(TrainingPhase.BASE);
        cycle.setCycleOrder(1);
        cycle.setStartWeek(1);
        cycle.setEndWeek(1);
        cycle.setStartDate(weekStart);
        cycle.setEndDate(weekStart.plusDays(6));
        cycle.setObjective("Base aeróbia");
        cycle.setExpectedProgression("Manter");
        cycle.setMaxWeeklyVolumeKm(15.0);
        seasonPlan.setTrainingCycles(new ArrayList<>(java.util.List.of(cycle)));
        seasonPlan = seasonPlanRepo.saveAndFlush(seasonPlan);

        when(generator.generate(any())).thenAnswer(invocation -> {
            WeeklyPlanGenerationContext context = invocation.getArgument(0);
            return new WeeklyPlanGenerationResult(WeeklyPlanFixtures.validProposal(context),
                    "resp_week", "gpt-test", 80, 160, 250L);
        });
    }

    @Test
    void persistsNestedWorkoutsAndVersionsWithoutGarminState() {
        WeeklyPlanCreateRequest request = new WeeklyPlanCreateRequest(seasonPlan.getId(), 1);
        WeeklyPlanResponse first = weeklyPlanService.create(athlete.getId(), request);
        WeeklyPlanResponse second = weeklyPlanService.create(athlete.getId(), request);

        assertEquals(1, first.version());
        assertEquals(2, second.version());
        assertEquals(15_000, first.plannedDistanceMeters());
        assertEquals(2, first.sessions().size());
        assertEquals(330, first.sessions().get(0).blocks().get(0).steps().get(0)
                .targetPaceFastestSecondsPerKm());
        assertEquals("resp_week", first.generation().responseId());
        assertEquals(2, weeklyPlanService.getHistory(athlete.getId(), seasonPlan.getId(), 1).size());
        assertEquals(2, weeklyPlanService.getLatest(athlete.getId(), seasonPlan.getId(), 1).version());
    }

    @Test
    void rejectsSeasonPlanThatIsNotApprovedBeforeCallingOpenAi() {
        seasonPlan.setStatus(SeasonPlanStatus.DRAFT);
        seasonPlanRepo.saveAndFlush(seasonPlan);

        assertThrows(WeeklyPlanPrerequisiteException.class, () -> weeklyPlanService.create(
                athlete.getId(), new WeeklyPlanCreateRequest(seasonPlan.getId(), 1)));
        verifyNoInteractions(generator);
        assertEquals(0, weeklyPlanRepo.count());
    }
}
