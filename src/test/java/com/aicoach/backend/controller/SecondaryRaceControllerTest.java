package com.aicoach.backend.controller;

import com.aicoach.backend.dto.SecondaryRaceResponse;
import com.aicoach.backend.enums.*;
import com.aicoach.backend.service.SecondaryRaceService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SecondaryRaceControllerTest {
    @Test
    void createsSecondaryRaceWithAdjustmentExplanation() throws Exception {
        SecondaryRaceService service = mock(SecondaryRaceService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new SecondaryRaceController(service)).build();
        when(service.create(eq(7L), any())).thenReturn(response());

        mvc.perform(post("/api/athletes/7/secondary-races").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"globalPlanId":9,"title":"5 km do bairro","raceDate":"2026-10-04",
                                "distanceMeters":5000,"targetTimeSeconds":1200,"priority":"B_RACE"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.primaryTarget.title").value("Maratona principal"))
                .andExpect(jsonPath("$.adjustments[0].action").value("REPLACE_WITH_RACE"))
                .andExpect(jsonPath("$.ruleVersion").value("secondary-race-rules-v1"));
    }

    @Test
    void listsRegisteredRaces() throws Exception {
        SecondaryRaceService service = mock(SecondaryRaceService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new SecondaryRaceController(service)).build();
        when(service.list(7L)).thenReturn(List.of(response()));

        mvc.perform(get("/api/athletes/7/secondary-races"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(20));
    }

    private SecondaryRaceResponse response() {
        LocalDate raceDate = LocalDate.of(2026, 10, 4);
        return new SecondaryRaceResponse(20L, 7L, 9L,
                new SecondaryRaceResponse.PrimaryTarget(3L, "Maratona principal",
                        LocalDate.of(2026, 12, 6), 42195, RacePriority.A_RACE),
                "5 km do bairro", raceDate, 5000, 1200, RacePriority.B_RACE,
                SecondaryRaceStatus.SCHEDULED, "secondary-race-rules-v1",
                raceDate.minusDays(5), raceDate.plusDays(4), "Ajuste determinístico",
                Instant.parse("2026-09-21T12:00:00Z"), List.of(
                new SecondaryRaceResponse.Adjustment(30L, 40L, RaceAdjustmentWindow.RACE_DAY,
                        SecondaryRaceAdjustmentAction.REPLACE_WITH_RACE, raceDate.minusDays(3),
                        raceDate, WorkoutType.QUALITY_1, WorkoutType.RACE, 7000, 5000,
                        2700, 1200, 100, "A prova substitui o treino de qualidade.")));
    }
}
