package com.aicoach.backend.service;

import com.aicoach.backend.dto.*;
import com.aicoach.backend.enums.*;
import com.aicoach.backend.models.*;
import com.aicoach.backend.repository.*;
import com.aicoach.backend.seasonplan.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@Transactional
class SeasonPlanPersistenceTest {
    @Autowired AthleteRepo athleteRepo;
    @Autowired ObjectiveRepo objectiveRepo;
    @Autowired AthleteMetricsRepo metricsRepo;
    @Autowired GlobalPlanRepo planRepo;
    @Autowired AthleteAssessmentService assessmentService;
    @Autowired SeasonPlanService seasonPlanService;
    @MockitoBean SeasonPlanGenerator generator;

    private Athlete athlete;
    private Objective objective;

    @BeforeEach
    void preparePrerequisites() {
        athlete = new Athlete();
        athlete.setName("Runner");
        athlete = athleteRepo.saveAndFlush(athlete);
        var assessment = assessmentService.createVersion(
                athlete.getId(), AthleteAssessmentServiceTest.validRequest(java.time.DayOfWeek.SUNDAY));

        objective = new Objective();
        objective.setAthlete(athlete);
        objective.setTitle(assessment.targetRace().title());
        objective.setTargetDate(assessment.targetRace().date());
        objective.setTargetDistance_m(assessment.targetRace().distanceMeters());
        objective.setTargetTimeMinutes(assessment.targetRace().desiredTimeSeconds() / 60);
        objective.setPriority(assessment.targetRace().priority());
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
        metricsRepo.saveAndFlush(metrics);

        when(generator.generate(any())).thenAnswer(invocation -> {
            SeasonPlanGenerationContext context = invocation.getArgument(0);
            return new SeasonPlanGenerationResult(SeasonPlanFixtures.validProposal(context),
                    "resp_test", "gpt-test", 100, 200, 300L);
        });
    }

    @Test
    void persistsVersionsAndSupersedesPreviouslyApprovedPlan() {
        SeasonPlanResponse first = seasonPlanService.create(
                athlete.getId(), new SeasonPlanCreateRequest(objective.getId()));
        assertEquals(SeasonPlanStatus.DRAFT, first.status());
        assertFalse(first.weeks().isEmpty());
        assertEquals(4, first.revisionCriteria().size());
        assertEquals("resp_test", first.generation().responseId());

        first = seasonPlanService.review(athlete.getId(), first.id(),
                new SeasonPlanReviewRequest(SeasonPlanReviewDecision.APPROVE, "Plano inicial aprovado"));
        SeasonPlanResponse second = seasonPlanService.create(
                athlete.getId(), new SeasonPlanCreateRequest(objective.getId()));
        second = seasonPlanService.review(athlete.getId(), second.id(),
                new SeasonPlanReviewRequest(SeasonPlanReviewDecision.APPROVE, null));

        assertEquals(1, first.version());
        assertEquals(2, second.version());
        assertEquals(SeasonPlanStatus.APPROVED, second.status());
        assertEquals(SeasonPlanStatus.SUPERSEDED, planRepo.findById(first.id()).orElseThrow().getStatus());
        assertEquals(2, seasonPlanService.getHistory(athlete.getId()).size());
    }

    @Test
    void neverPersistsProposalRejectedByDeterministicValidation() {
        doAnswer(invocation -> {
            SeasonPlanGenerationContext context = invocation.getArgument(0);
            var valid = SeasonPlanFixtures.validProposal(context);
            var invalid = new SeasonPlanProposal(valid.summary(), valid.phases(), valid.weeks(), java.util.List.of());
            return new SeasonPlanGenerationResult(invalid, "resp_bad", "gpt-test", 1, 1, 1L);
        }).when(generator).generate(any());

        assertThrows(SeasonPlanValidationException.class, () -> seasonPlanService.create(
                athlete.getId(), new SeasonPlanCreateRequest(objective.getId())));
        assertEquals(0, planRepo.count());
    }
}
