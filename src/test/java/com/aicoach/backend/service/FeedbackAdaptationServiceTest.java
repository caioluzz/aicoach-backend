package com.aicoach.backend.service;

import com.aicoach.backend.adaptation.AdaptationRuleEngine;
import com.aicoach.backend.dto.FeedbackRequest;
import com.aicoach.backend.enums.*;
import com.aicoach.backend.models.*;
import com.aicoach.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FeedbackAdaptationServiceTest {
    private AthleteRepo athleteRepo;
    private ActivityRepo activityRepo;
    private ActivityComparisonRepo comparisonRepo;
    private AthleteFeedbackRepo feedbackRepo;
    private AdaptationDecisionRepo decisionRepo;
    private PlannedActivityRepo plannedActivityRepo;
    private FeedbackAdaptationService service;

    @BeforeEach
    void setUp() {
        athleteRepo = mock(AthleteRepo.class);
        activityRepo = mock(ActivityRepo.class);
        comparisonRepo = mock(ActivityComparisonRepo.class);
        feedbackRepo = mock(AthleteFeedbackRepo.class);
        decisionRepo = mock(AdaptationDecisionRepo.class);
        plannedActivityRepo = mock(PlannedActivityRepo.class);
        service = new FeedbackAdaptationService(athleteRepo, activityRepo, comparisonRepo,
                feedbackRepo, decisionRepo, plannedActivityRepo, new AdaptationRuleEngine(),
                Clock.fixed(Instant.parse("2026-09-21T12:00:00Z"), ZoneOffset.UTC));

        Athlete athlete = new Athlete();
        athlete.setId(7L);
        when(athleteRepo.findById(7L)).thenReturn(Optional.of(athlete));
        when(feedbackRepo.save(any())).thenAnswer(invocation -> {
            AthleteFeedback value = invocation.getArgument(0);
            value.setId(10L);
            return value;
        });
        when(decisionRepo.save(any())).thenAnswer(invocation -> {
            AdaptationDecision value = invocation.getArgument(0);
            value.setId(11L);
            return value;
        });
        when(plannedActivityRepo.findUnexecutedUpcoming(anyLong(), any(), any(), any()))
                .thenReturn(List.of());
    }

    @Test
    void combinesActivityExecutionWithFeedbackAndReviewsOnlyFutureUnexecutedSessions() {
        Activity activity = new Activity();
        activity.setId(44L);
        activity.setStartedAt(LocalDateTime.of(2026, 9, 20, 7, 0));
        when(activityRepo.findByIdAndAthleteId(44L, 7L)).thenReturn(Optional.of(activity));
        ActivityComparison comparison = new ActivityComparison();
        comparison.setId(8L);
        comparison.setActivity(activity);
        comparison.setClassification(ComplianceClassification.PARTIAL);
        comparison.setCompliancePercentage(new BigDecimal("68.00"));
        when(comparisonRepo.findByActivityId(44L)).thenReturn(Optional.of(comparison));
        PlannedActivity quality = planned(55L, LocalDate.of(2026, 9, 22), WorkoutType.QUALITY_1);
        when(plannedActivityRepo.findUnexecutedUpcoming(eq(7L), eq(LocalDate.of(2026, 9, 21)),
                eq(LocalDate.of(2026, 9, 27)), any())).thenReturn(List.of(quality));

        var response = service.submit(7L, new FeedbackRequest(44L, LocalDate.of(2026, 9, 20),
                9, 6, 7.0, 0, null, FeedbackFeeling.BAD, "Muito mais difícil que o esperado"));

        assertEquals(AdaptationAlertLevel.REDUCE_LOAD, response.alertLevel());
        assertEquals(25, response.loadReductionPercent());
        assertEquals(WorkoutAdjustmentAction.REPLACE_WITH_EASY,
                response.upcomingWorkoutReview().get(0).action());
        assertEquals(4500, response.upcomingWorkoutReview().get(0).proposedDistanceMeters());
        assertEquals(8L, response.evidence().comparisonId());
    }

    @Test
    void dayFeedbackDoesNotNeedActivityAndDoesNotProposeGeneralReplanForIsolatedFatigue() {
        var response = service.submit(7L, new FeedbackRequest(null, LocalDate.of(2026, 9, 21),
                7, 8, 5.5, 0, null, FeedbackFeeling.BAD, "Noite ruim"));

        assertEquals(35, response.loadReductionPercent());
        assertNull(response.feedback().activityId());
        assertFalse(response.seasonPlanReviewProposed());
        assertNull(response.evidence().comparisonId());
    }

    @Test
    void requiresPainLocation() {
        FeedbackRequest request = new FeedbackRequest(null, LocalDate.of(2026, 9, 21),
                5, 5, 8.0, 3, " ", FeedbackFeeling.NEUTRAL, null);

        assertThrows(FeedbackValidationException.class, () -> service.submit(7L, request));
        verifyNoInteractions(decisionRepo);
    }

    private PlannedActivity planned(Long id, LocalDate date, WorkoutType type) {
        PlannedActivity activity = new PlannedActivity();
        activity.setId(id);
        activity.setScheduledDate(date);
        activity.setWorkoutType(type);
        activity.setPlannedDistanceMeters(6000);
        activity.setPlannedDurationSeconds(2400);
        activity.setCalculatedStressPoints(80);
        return activity;
    }
}
