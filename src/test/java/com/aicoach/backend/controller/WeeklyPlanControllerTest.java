package com.aicoach.backend.controller;

import com.aicoach.backend.dto.*;
import com.aicoach.backend.service.GarminWorkoutDeliveryService;
import com.aicoach.backend.service.WeeklyPlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WeeklyPlanControllerTest {
    private WeeklyPlanService service;
    private GarminWorkoutDeliveryService garminDeliveryService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(WeeklyPlanService.class);
        garminDeliveryService = mock(GarminWorkoutDeliveryService.class);
        mvc = MockMvcBuilders.standaloneSetup(
                new WeeklyPlanController(service, garminDeliveryService)).build();
    }

    @Test
    void exposesReviewRegenerationAndManualEditEndpoints() throws Exception {
        mvc.perform(post("/api/athletes/7/weekly-plans/31/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVE\",\"comment\":\"Revisado\"}"))
                .andExpect(status().isOk());
        verify(service).review(eq(7L), eq(31L), any(WeeklyPlanReviewRequest.class));

        mvc.perform(post("/api/athletes/7/weekly-plans/31/regenerate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Alterar distribuição\"}"))
                .andExpect(status().isCreated());
        verify(service).regenerate(eq(7L), eq(31L), any(WeeklyPlanRegenerateRequest.class));

        mvc.perform(post("/api/athletes/7/weekly-plans/31/edits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "summary":"Semana revisada",
                                  "reason":"Ajustar agenda",
                                  "sessions":[{
                                    "order":1,
                                    "name":"Rodagem",
                                    "scheduledDate":"2026-10-06",
                                    "workoutType":"EASY_RUN",
                                    "blocks":[{"repetitions":1,"steps":[{
                                      "kind":"WORK",
                                      "durationType":"DISTANCE",
                                      "durationValue":5000,
                                      "targetZone":"E_PACE",
                                      "instruction":"Confortável"
                                    }]}]
                                  }]
                                }
                                """))
                .andExpect(status().isCreated());
        verify(service).edit(eq(7L), eq(31L), any(WeeklyPlanEditRequest.class));
    }

    @Test
    void rejectsBlankReplacementReasonBeforeCallingService() throws Exception {
        mvc.perform(post("/api/athletes/7/weekly-plans/31/regenerate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\" \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void exposesGarminPreviewDeliveryConfirmationUpdateAndCancellation() throws Exception {
        mvc.perform(get("/api/athletes/7/weekly-plans/31/garmin/preview"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/athletes/7/weekly-plans/31/garmin/deliveries"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/athletes/7/weekly-plans/31/garmin/confirmations"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/athletes/7/weekly-plans/31/garmin/deliveries/91"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/athletes/7/weekly-plans/31/garmin/deliveries/91"))
                .andExpect(status().isOk());

        verify(garminDeliveryService).preview(7L, 31L);
        verify(garminDeliveryService).deliver(7L, 31L);
        verify(garminDeliveryService).confirm(7L, 31L);
        verify(garminDeliveryService).update(7L, 31L, 91L);
        verify(garminDeliveryService).cancel(7L, 31L, 91L);
    }
}
