package com.aicoach.backend.repository;

import com.aicoach.backend.models.Objective;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObjectiveRepo extends JpaRepository<Objective, Long> {
    List<Objective> findByAthleteId(Long athleteId);
}