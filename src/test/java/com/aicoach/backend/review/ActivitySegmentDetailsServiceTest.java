package com.aicoach.backend.review;

import com.aicoach.backend.dto.ActivitySegmentDetailRequest;
import com.aicoach.backend.dto.ActivitySegmentDetails;
import com.aicoach.backend.enums.*;
import com.aicoach.backend.models.*;
import com.aicoach.backend.repository.ActivityRecordRepo;
import com.aicoach.backend.repository.ActivityReviewRepo;
import com.aicoach.backend.repository.ActivitySegmentDetailRequestRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ActivitySegmentDetailsServiceTest {
    private ActivitySegmentDetailRequestRepo requestRepo;
    private ActivityRecordRepo recordRepo;
    private ActivitySegmentDetailsService service;
    private ActivityReview review;
    private final List<com.aicoach.backend.models.ActivitySegmentDetailRequest> audits = new ArrayList<>();

    @BeforeEach
    void setUp() {
        ActivityReviewRepo reviewRepo = mock(ActivityReviewRepo.class);
        requestRepo = mock(ActivitySegmentDetailRequestRepo.class);
        recordRepo = mock(ActivityRecordRepo.class);
        service = new ActivitySegmentDetailsService(reviewRepo, requestRepo, recordRepo,
                Clock.fixed(Instant.parse("2026-09-20T12:00:00Z"), ZoneOffset.UTC));
        review = review();
        when(reviewRepo.findById(9L)).thenReturn(Optional.of(review));
        when(requestRepo.findByReviewIdOrderByRequestedAtAsc(9L)).thenAnswer(ignored -> List.copyOf(audits));
        when(requestRepo.saveAndFlush(any())).thenAnswer(invocation -> saveAudit(invocation.getArgument(0)));
        when(requestRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(recordRepo.findForActivityOrdered(44L)).thenReturn(records());
    }

    @Test
    void returnsTenSecondBucketsWithOnlyRequestedFieldsAndAuditsReason() {
        ActivitySegmentDetails details = service.getActivitySegmentDetails(9L,
                request(SegmentResolution.TEN_SECONDS, 10, 25, "Investigar queda de ritmo neste trecho"));

        assertEquals(4, details.points().size());
        assertNotNull(details.points().get(0).paceSecondsPerKm());
        assertNotNull(details.points().get(0).heartRate());
        assertNull(details.points().get(0).cadence());
        assertEquals(new BigDecimal("0"), details.returnedStart());
        assertEquals(1, audits.size());
        assertEquals("Investigar queda de ritmo neste trecho", audits.get(0).getReason());
        assertEquals(4, audits.get(0).getPointsReturned());
    }

    @Test
    void refusesRawTelemetryWithoutPriorFiveSecondInspection() {
        SegmentDetailValidationException exception = assertThrows(SegmentDetailValidationException.class,
                () -> service.getActivitySegmentDetails(9L,
                        request(SegmentResolution.RAW, 10, 20, "Confirmar oscilação abrupta do sensor")));

        assertTrue(exception.getMessage().contains("FIVE_SECONDS"));
        verify(recordRepo, never()).findForActivityOrdered(any());
        verify(requestRepo, never()).saveAndFlush(any());
    }

    @Test
    void permitsFiveSecondsThenRawOnlyAfterOverlappingCoarserRequests() {
        service.getActivitySegmentDetails(9L,
                request(SegmentResolution.TEN_SECONDS, 10, 30, "Localizar início da desaceleração observada"));
        service.getActivitySegmentDetails(9L,
                request(SegmentResolution.FIVE_SECONDS, 12, 25, "Refinar a transição identificada em dez segundos"));
        ActivitySegmentDetails raw = service.getActivitySegmentDetails(9L,
                request(SegmentResolution.RAW, 15, 20, "Confirmar se a transição é real ou ruído"));

        assertFalse(raw.points().isEmpty());
        assertEquals(List.of(SegmentResolution.TEN_SECONDS, SegmentResolution.FIVE_SECONDS, SegmentResolution.RAW),
                audits.stream().map(com.aicoach.backend.models.ActivitySegmentDetailRequest::getResolution).toList());
    }

    @Test
    void supportsKilometerRangesAndAltitudeSelection() {
        ActivitySegmentDetailRequest request = new ActivitySegmentDetailRequest(SegmentQueryType.KILOMETER,
                new BigDecimal("0.050"), new BigDecimal("0.150"), SegmentResolution.TEN_SECONDS,
                List.of(TelemetryField.ALTITUDE), new BigDecimal("0.010"), new BigDecimal("0.010"),
                "Verificar influência da elevação na perda de ritmo");

        ActivitySegmentDetails details = service.getActivitySegmentDetails(9L, request);

        assertFalse(details.points().isEmpty());
        assertNotNull(details.points().get(0).altitudeMeters());
        assertNull(details.points().get(0).paceSecondsPerKm());
        assertEquals(SegmentQueryType.KILOMETER, details.queryType());
    }

    @Test
    void returnsPersistedStepAggregatesWithoutReadingRawRecords() {
        ActivityStepComparison step = new ActivityStepComparison();
        step.setSequenceNumber(2);
        step.setClassification(ComplianceClassification.PARTIAL);
        step.setCompliancePercentage(new BigDecimal("70.00"));
        step.setIntervalStartSeconds(new BigDecimal("10.00"));
        step.setIntervalEndSeconds(new BigDecimal("30.00"));
        step.setActualPaceSecondsPerKm(new BigDecimal("380.00"));
        step.setExplanation("ritmo abaixo da faixa");
        review.getComparison().getSteps().add(step);

        ActivitySegmentDetails details = service.getActivitySegmentDetails(9L,
                request(SegmentResolution.STEP, 10, 30, "Revisar a etapa parcial já agregada"));

        assertEquals(1, details.steps().size());
        assertEquals(2, details.steps().get(0).sequence());
        verify(recordRepo, never()).findForActivityOrdered(any());
    }

    private com.aicoach.backend.models.ActivitySegmentDetailRequest saveAudit(
            com.aicoach.backend.models.ActivitySegmentDetailRequest audit) {
        audit.setId((long) audits.size() + 1);
        audits.add(audit);
        return audit;
    }

    private ActivitySegmentDetailRequest request(SegmentResolution resolution, int start, int end, String reason) {
        return new ActivitySegmentDetailRequest(SegmentQueryType.TIME, BigDecimal.valueOf(start),
                BigDecimal.valueOf(end), resolution, List.of(TelemetryField.PACE, TelemetryField.HEART_RATE),
                BigDecimal.valueOf(10), BigDecimal.valueOf(10), reason);
    }

    private ActivityReview review() {
        Activity activity = new Activity();
        activity.setId(44L);
        ActivityComparison comparison = new ActivityComparison();
        comparison.setId(8L);
        comparison.setActivity(activity);
        ActivityReview item = new ActivityReview();
        item.setId(9L);
        item.setActivity(activity);
        item.setComparison(comparison);
        item.setStatus(ActivityReviewStatus.IN_PROGRESS);
        return item;
    }

    private List<ActivityRecord> records() {
        List<ActivityRecord> result = new ArrayList<>();
        for (int second = 0; second <= 40; second += 5) {
            ActivityRecord record = new ActivityRecord();
            record.setId(new ActivityRecordId(44L,
                    LocalDateTime.of(2026, 9, 20, 7, 0).plusSeconds(second)));
            record.setElapsedS((short) second);
            record.setDistanceKm(BigDecimal.valueOf(second / 180.0));
            record.setPaceSPerKm((short) (350 + second));
            record.setHeartRate((short) (140 + second));
            record.setCadence((short) 86);
            record.setAltitudeM(BigDecimal.valueOf(100 + second / 2.0));
            result.add(record);
        }
        return result;
    }
}
