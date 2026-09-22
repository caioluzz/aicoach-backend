package com.aicoach.backend.controller;

import com.aicoach.backend.api.AthleteApi;
import com.aicoach.backend.dto.AthleteRequestDTO;
import com.aicoach.backend.dto.AthleteSummaryResponse;
import com.aicoach.backend.dto.CreatedAthleteResponseDTO;
import com.aicoach.backend.models.Athlete;
import com.aicoach.backend.service.AthleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AthleteController implements AthleteApi {

    private final AthleteService athleteService;

    @Override
    public CreatedAthleteResponseDTO createAthlete(AthleteRequestDTO request) {
        Athlete savedAthlete = athleteService.saveAthlete(request);
        return new CreatedAthleteResponseDTO(savedAthlete.getId(), "Atleta cadastrado com sucesso!");
    }

    @Override
    public List<AthleteSummaryResponse> getAllAthletes() {
        return athleteService.getAllAthletes();
    }
}
