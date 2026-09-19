package com.aicoach.backend.repository;

import com.aicoach.backend.models.AthleteAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AthleteAssessmentRepo extends JpaRepository<AthleteAssessment, Long> {
    Optional<AthleteAssessment> findTopByAthleteIdOrderByVersionDesc(Long athleteId);
    List<AthleteAssessment> findByAthleteIdOrderByVersionDesc(Long athleteId);
}
