package com.aicoach.backend.service;

import com.aicoach.backend.models.Athlete;
import com.aicoach.backend.repository.AthleteRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AthleteService {

    private final AthleteRepo athleteRepo;

    public Athlete saveAthlete(Athlete athlete) {
        return athleteRepo.save(athlete);
    }

    public List<Athlete> getAllAthletes() {
        return athleteRepo.findAll();
    }
}
