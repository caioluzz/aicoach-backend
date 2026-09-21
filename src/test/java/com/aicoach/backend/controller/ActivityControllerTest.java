package com.aicoach.backend.controller;

import com.aicoach.backend.dto.ActivitySyncResponse;
import com.aicoach.backend.dto.ActivityComparisonResponse;
import com.aicoach.backend.dto.ActivityReviewResponse;
import com.aicoach.backend.enums.ActivityMatchType;
import com.aicoach.backend.enums.ActivityReviewStatus;
import com.aicoach.backend.enums.ComplianceClassification;
import com.aicoach.backend.enums.ActivitySyncStatus;
import com.aicoach.backend.service.ActivityService;
import com.aicoach.backend.service.ActivityComparisonService;
import com.aicoach.backend.service.ActivityReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ActivityControllerTest {
    private ActivityService service;
    private ActivityComparisonService comparisonService;
    private ActivityReviewService reviewService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(ActivityService.class);
        comparisonService = mock(ActivityComparisonService.class);
        reviewService = mock(ActivityReviewService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ActivityController(service, comparisonService, reviewService)).build();
    }

    @Test
    void runsManualSyncForOneAthlete() throws Exception {
        ActivitySyncResponse response = response();
        when(service.syncGarminActivities(7L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/activities/sync/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.athleteId").value(7))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.importedCount").value(1));
    }

    @Test
    void exposesLatestSyncStatus() throws Exception {
        when(service.getSyncStatuses()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/v1/activities/sync/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].checkpointStartedAt").value("2026-09-18T06:30:00"));
    }

    @Test
    void exposesPersistedDeterministicComparison() throws Exception {
        ActivityComparisonResponse comparison = new ActivityComparisonResponse(
                9L, 7L, 44L, 55L, ActivityMatchType.PLANNED, ComplianceClassification.FULFILLED,
                new BigDecimal("96.50"), "activity-comparison-v1",
                new ActivityComparisonResponse.Metrics(1800, new BigDecimal("1810.00"),
                        5000, new BigDecimal("5005.00"), new BigDecimal("360.00"),
                        new BigDecimal("361.64"),
                        new BigDecimal("158.00"), new BigDecimal("87.00")),
                "explicável", Instant.parse("2026-09-19T18:00:00Z"), List.of());
        when(comparisonService.getForActivity(44L)).thenReturn(comparison);

        mockMvc.perform(get("/api/v1/activities/44/comparison"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compliancePercentage").value(96.50))
                .andExpect(jsonPath("$.toleranceVersion").value("activity-comparison-v1"));
    }

    @Test
    void startsCoachReviewWithoutExposingInternalTelemetryTool() throws Exception {
        ActivityReviewResponse response = new ActivityReviewResponse(10L, 44L, 9L,
                ActivityReviewStatus.COMPLETED, "activity-review-policy-v1", "discrepância relevante",
                true, "Houve perda de ritmo no trecho final.", "gpt-test", "resp_10",
                100, 20, 75L, Instant.parse("2026-09-20T18:00:00Z"),
                Instant.parse("2026-09-20T18:00:01Z"), List.of());
        when(reviewService.review(44L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/activities/44/review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.assessment").value("Houve perda de ritmo no trecho final."));
    }

    private ActivitySyncResponse response() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 9, 18, 6, 30);
        return new ActivitySyncResponse(
                7L, ActivitySyncStatus.SUCCESS, timestamp, timestamp, timestamp,
                2, 1, 1, null);
    }
}
