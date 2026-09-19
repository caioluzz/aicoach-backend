package com.aicoach.backend.repository;

import com.aicoach.backend.models.Objective;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ObjectiveRepo extends JpaRepository<Objective, Long> {
    List<Objective> findByAthleteId(Long athleteId);
    Optional<Objective> findByIdAndAthleteId(Long id, Long athleteId);
}
