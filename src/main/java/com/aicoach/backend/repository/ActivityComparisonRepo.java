package com.aicoach.backend.repository;

import com.aicoach.backend.models.ActivityComparison;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityComparisonRepo extends JpaRepository<ActivityComparison, Long> {
    Optional<ActivityComparison> findByActivityId(Long activityId);
    Optional<ActivityComparison> findByPlannedActivityId(Long plannedActivityId);
    List<ActivityComparison> findByAthleteIdOrderByCalculatedAtDesc(Long athleteId);
    boolean existsByPlannedActivityId(Long plannedActivityId);
}
