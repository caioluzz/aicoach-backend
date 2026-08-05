package com.aicoach.backend.repository;

import com.aicoach.backend.models.AthleteMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AthleteMetricsRepo extends JpaRepository<AthleteMetrics, Long> {
    List<AthleteMetrics> findByAthleteId(Long athleteId);

    Optional<AthleteMetrics> findTopByAthleteIdOrderByRecordedAtDesc(Long athleteId);
}