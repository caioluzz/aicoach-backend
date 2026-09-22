package com.aicoach.backend.controller;

import com.aicoach.backend.dto.AthleteSummaryResponse;
import com.aicoach.backend.service.AthleteService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AthleteControllerTest {
    @Test
    void listsOnlyTheSafeAthleteSummaryWithoutJpaRelationshipsOrSecrets() throws Exception {
        AthleteService service = mock(AthleteService.class);
        when(service.getAllAthletes()).thenReturn(List.of(new AthleteSummaryResponse(
                7L, "Caio Luz", "caio@example.com", LocalDate.of(1990, 4, 10))));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AthleteController(service)).build();

        mvc.perform(get("/api/athletes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].name").value("Caio Luz"))
                .andExpect(jsonPath("$[0].garminPassword").doesNotExist())
                .andExpect(jsonPath("$[0].objectives").doesNotExist());
    }
}
