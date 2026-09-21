package com.aicoach.backend.repository;

import com.aicoach.backend.models.SecondaryRace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecondaryRaceRepo extends JpaRepository<SecondaryRace, Long> {
    List<SecondaryRace> findByAthleteIdOrderByRaceDate(Long athleteId);
    boolean existsByGlobalPlanIdAndRaceDate(Long globalPlanId, java.time.LocalDate raceDate);
}
