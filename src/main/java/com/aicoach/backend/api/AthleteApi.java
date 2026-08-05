package com.aicoach.backend.api;

import com.aicoach.backend.dto.AthleteRequestDTO;
import com.aicoach.backend.dto.CreatedAthleteResponseDTO;
import com.aicoach.backend.models.Athlete;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/athletes")
public interface AthleteApi {

    @PostMapping("/new")
    @ResponseStatus(HttpStatus.CREATED)
    CreatedAthleteResponseDTO createAthlete(@RequestBody AthleteRequestDTO request);

    @GetMapping
    List<Athlete> getAllAthletes();
}