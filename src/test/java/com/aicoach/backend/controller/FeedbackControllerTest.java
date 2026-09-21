package com.aicoach.backend.controller;

import com.aicoach.backend.dto.AdaptationDecisionResponse;
import com.aicoach.backend.enums.*;
import com.aicoach.backend.service.FeedbackAdaptationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FeedbackControllerTest {
    @Test
    void registersFeedbackAndReturnsAuditableDecision() throws Exception {
        FeedbackAdaptationService service = mock(FeedbackAdaptationService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new FeedbackController(service)).build();
        when(service.submit(eq(7L), any())).thenReturn(response());

        mvc.perform(post("/api/athletes/7/feedback").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"feedbackDate":"2026-09-21","perceivedEffort":8,"fatigue":8,
                                "sleepHours":5.5,"painSeverity":0,"feeling":"BAD","comment":"cansado"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ruleVersion").value("feedback-adaptation-rules-v1"))
                .andExpect(jsonPath("$.alertLevel").value("REDUCE_LOAD"));
    }

    @Test
    void exposesDecisionHistory() throws Exception {
        FeedbackAdaptationService service = mock(FeedbackAdaptationService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new FeedbackController(service)).build();
        when(service.history(7L)).thenReturn(List.of(response()));

        mvc.perform(get("/api/athletes/7/feedback/decisions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11));
    }

    private AdaptationDecisionResponse response() {
        return new AdaptationDecisionResponse(11L, 7L,
                new AdaptationDecisionResponse.Feedback(10L, null, LocalDate.of(2026, 9, 21),
                        8, 8, 5.5, 0, null, FeedbackFeeling.BAD, "cansado"),
                new AdaptationDecisionResponse.Evidence(null, null, null,
                        "rpe=8;fatigue=8;sleepHours=5.5;pain=0;execution=none;recentReductions=0"),
                "feedback-adaptation-rules-v1", AdaptationAlertLevel.REDUCE_LOAD,
                35, false, true, false, null, "Reduzir carga", Instant.parse("2026-09-21T12:00:00Z"),
                List.of());
    }
}
