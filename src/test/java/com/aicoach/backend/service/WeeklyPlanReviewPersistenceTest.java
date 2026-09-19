package com.aicoach.backend.service;

import com.aicoach.backend.dto.*;
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
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class WeeklyPlanReviewPersistenceTest {
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
        athlete.setName("Runner review");
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
                    "resp_review", "gpt-test", 80, 160, 250L);
        });
    }

    @Test
    void approvesOneVersionAndSupersedesPreviouslyApprovedVersion() {
        WeeklyPlanResponse first = create();
        first = weeklyPlanService.review(athlete.getId(), first.id(),
                new WeeklyPlanReviewRequest(WeeklyPlanReviewDecision.APPROVE, "Aprovado"));
        WeeklyPlanResponse second = create();
        second = weeklyPlanService.review(athlete.getId(), second.id(),
                new WeeklyPlanReviewRequest(WeeklyPlanReviewDecision.APPROVE, null));

        assertEquals(WeeklyPlanStatus.APPROVED, second.status());
        assertEquals(WeeklyPlanStatus.SUPERSEDED,
                weeklyPlanRepo.findById(first.id()).orElseThrow().getStatus());
        Long approvedId = second.id();
        assertThrows(WeeklyPlanStateException.class, () -> weeklyPlanService.review(
                athlete.getId(), approvedId,
                new WeeklyPlanReviewRequest(WeeklyPlanReviewDecision.REJECT, null)));
    }

    @Test
    void requestsNewProposalWithoutMutatingTheOriginalContents() {
        WeeklyPlanResponse first = create();
        WeeklyPlanResponse replacement = weeklyPlanService.regenerate(athlete.getId(), first.id(),
                new WeeklyPlanRegenerateRequest("Trocar o dia do treino de qualidade"));

        WeeklyPlan original = weeklyPlanRepo.findById(first.id()).orElseThrow();
        assertEquals(WeeklyPlanStatus.REJECTED, original.getStatus());
        assertEquals("Trocar o dia do treino de qualidade", original.getReviewComment());
        assertEquals(2, replacement.version());
        assertEquals(first.id(), replacement.sourceWeeklyPlanId());
        assertEquals(WeeklyPlanStatus.VALIDATED, replacement.status());
        assertEquals(first.sessions(), weeklyPlanService.get(athlete.getId(), first.id()).sessions());
    }

    @Test
    void createsValidatedManualVersionAndRecalculatesProtectedFields() {
        WeeklyPlanResponse first = create();
        WeeklyPlanEditRequest edit = toEdit(first, "Ajuste manual revisado");
        WeeklyPlanResponse replacement = weeklyPlanService.edit(athlete.getId(), first.id(), edit);

        assertEquals(2, replacement.version());
        assertEquals("MANUAL", replacement.generation().source());
        assertEquals("manual", replacement.generation().model());
        assertNull(replacement.generation().responseId());
        assertEquals(first.plannedDistanceMeters(), replacement.plannedDistanceMeters());
        assertEquals(330, replacement.sessions().get(0).blocks().get(0).steps().get(0)
                .targetPaceFastestSecondsPerKm());
        assertEquals(WeeklyPlanStatus.REJECTED,
                weeklyPlanRepo.findById(first.id()).orElseThrow().getStatus());
    }

    @Test
    void rejectsInvalidManualEditAndKeepsSourcePending() {
        WeeklyPlanResponse first = create();
        WeeklyPlanEditRequest valid = toEdit(first, "Data inválida para teste");
        var session = valid.sessions().get(0);
        var invalidSession = new WeeklyPlanEditRequest.Session(session.order(), session.name(),
                first.weekEnd().plusDays(1), session.workoutType(), session.blocks());
        var sessions = new ArrayList<>(valid.sessions());
        sessions.set(0, invalidSession);
        WeeklyPlanEditRequest invalid = new WeeklyPlanEditRequest(valid.summary(), sessions, valid.reason());

        assertThrows(WeeklyPlanValidationException.class,
                () -> weeklyPlanService.edit(athlete.getId(), first.id(), invalid));
        assertEquals(WeeklyPlanStatus.VALIDATED,
                weeklyPlanRepo.findById(first.id()).orElseThrow().getStatus());
        assertEquals(1, weeklyPlanRepo.count());
    }

    private WeeklyPlanResponse create() {
        return weeklyPlanService.create(athlete.getId(), new WeeklyPlanCreateRequest(seasonPlan.getId(), 1));
    }

    private WeeklyPlanEditRequest toEdit(WeeklyPlanResponse response, String reason) {
        return new WeeklyPlanEditRequest(response.summary(), response.sessions().stream().map(session ->
                new WeeklyPlanEditRequest.Session(session.order(), session.name(), session.scheduledDate(),
                        session.workoutType(), session.blocks().stream().map(block ->
                        new WeeklyPlanEditRequest.Block(block.repetitions(), block.steps().stream().map(step ->
                                new WeeklyPlanEditRequest.Step(step.kind(), step.durationType(), step.durationValue(),
                                        step.targetZone(), step.instruction())).toList())).toList())).toList(), reason);
    }
}
