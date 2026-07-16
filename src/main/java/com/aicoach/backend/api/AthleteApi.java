package com.aicoach.backend.api;

import com.aicoach.backend.domain.Athlete;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/athletes")
public interface AthleteApi {

    @PostMapping("/new")
    @ResponseStatus(HttpStatus.CREATED)
    Athlete createAthlete(@RequestBody Athlete request);

    @GetMapping
    List<Athlete> getAllAthletes();
}