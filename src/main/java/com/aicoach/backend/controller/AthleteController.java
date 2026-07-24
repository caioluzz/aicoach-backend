package com.aicoach.backend.controller;

import com.aicoach.backend.api.AthleteApi;
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
    public Athlete createAthlete(Athlete request) {
        return athleteService.saveAthlete(request);
    }

    @Override
    public List<Athlete> getAllAthletes() {
        return athleteService.getAllAthletes();
    }
}