package com.aicoach.backend.controller;

import com.aicoach.backend.dto.ActivitySyncResponse;
import com.aicoach.backend.enums.ActivitySyncStatus;
import com.aicoach.backend.service.ActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ActivityControllerTest {
    private ActivityService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(ActivityService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ActivityController(service)).build();
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

    private ActivitySyncResponse response() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 9, 18, 6, 30);
        return new ActivitySyncResponse(
                7L, ActivitySyncStatus.SUCCESS, timestamp, timestamp, timestamp,
                2, 1, 1, null);
    }
}
