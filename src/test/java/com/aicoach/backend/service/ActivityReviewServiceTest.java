package com.aicoach.backend.service;

import com.aicoach.backend.enums.*;
import com.aicoach.backend.models.*;
import com.aicoach.backend.repository.ActivityComparisonRepo;
import com.aicoach.backend.repository.ActivityReviewRepo;
import com.aicoach.backend.review.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ActivityReviewServiceTest {
    private ActivityComparisonRepo comparisonRepo;
    private ActivityReviewRepo reviewRepo;
    private ActivityComparisonService comparisonService;
    private ActivityCoach coach;
    private ActivityReviewService service;

    @BeforeEach
    void setUp() {
        comparisonRepo = mock(ActivityComparisonRepo.class);
        reviewRepo = mock(ActivityReviewRepo.class);
        comparisonService = mock(ActivityComparisonService.class);
        coach = mock(ActivityCoach.class);
        ActivitySegmentDetailsService detailsService = mock(ActivitySegmentDetailsService.class);
        service = new ActivityReviewService(comparisonRepo, reviewRepo, comparisonService,
                new ActivityReviewPolicy(), coach, detailsService,
                Clock.fixed(Instant.parse("2026-09-20T18:00:00Z"), ZoneOffset.UTC));
        when(reviewRepo.findByActivityId(44L)).thenReturn(Optional.empty());
        when(reviewRepo.save(any())).thenAnswer(invocation -> assignId(invocation.getArgument(0)));
        when(reviewRepo.saveAndFlush(any())).thenAnswer(invocation -> assignId(invocation.getArgument(0)));
    }

    @Test
    void skipsModelForTrivialActivityAndPersistsReason() {
        when(comparisonRepo.findByActivityId(44L)).thenReturn(Optional.of(comparison(500, 800,
                ComplianceClassification.DIFFERENT, "20.00")));

        var response = service.review(44L);

        assertEquals(ActivityReviewStatus.SKIPPED, response.status());
        assertFalse(response.modelCalled());
        assertTrue(response.policyReason().contains("trivial"));
        verifyNoInteractions(coach);
    }

    @Test
    void sendsOnlyDeterministicSummaryToCoachForRelevantActivity() {
        ActivityComparison comparison = comparison(2400, 6000, ComplianceClassification.PARTIAL, "70.00");
        when(comparisonRepo.findByActivityId(44L)).thenReturn(Optional.of(comparison));
        when(coach.review(any(), any())).thenReturn(new ActivityCoach.Result(
                "A segunda metade perdeu ritmo, sem aumento correspondente de FC.",
                "resp_9", "gpt-test", 120, 35, 80L));

        var response = service.review(44L);

        assertEquals(ActivityReviewStatus.COMPLETED, response.status());
        assertTrue(response.modelCalled());
        assertEquals("resp_9", response.responseId());
        verify(comparisonService).getForActivity(44L);
        verify(coach).review(argThat(context -> context.activityId().equals(44L)
                && context.distanceMeters().equals(6000.0)), any());
    }

    private ActivityReview assignId(ActivityReview review) {
        review.setId(9L);
        return review;
    }

    private ActivityComparison comparison(double duration, double distance,
                                          ComplianceClassification classification, String percentage) {
        Activity activity = new Activity();
        activity.setId(44L);
        activity.setStartedAt(LocalDateTime.of(2026, 9, 20, 7, 0));
        activity.setSport("running");
        activity.setDurationSeconds(duration);
        activity.setDistanceMeters(distance);
        ActivityComparison comparison = new ActivityComparison();
        comparison.setId(8L);
        comparison.setActivity(activity);
        comparison.setClassification(classification);
        comparison.setCompliancePercentage(new BigDecimal(percentage));
        return comparison;
    }
}
