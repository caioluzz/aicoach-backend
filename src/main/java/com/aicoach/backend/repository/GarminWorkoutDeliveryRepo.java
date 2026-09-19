package com.aicoach.backend.repository;

import com.aicoach.backend.models.GarminWorkoutDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GarminWorkoutDeliveryRepo extends JpaRepository<GarminWorkoutDelivery, Long> {
    Optional<GarminWorkoutDelivery> findByAthleteIdAndPlannedActivityIdAndPlanVersionAndContentHash(
            Long athleteId, Long plannedActivityId, Integer planVersion, String contentHash);
    Optional<GarminWorkoutDelivery> findByIdAndAthleteIdAndWeeklyPlanId(
            Long id, Long athleteId, Long weeklyPlanId);
    List<GarminWorkoutDelivery> findByAthleteIdAndWeeklyPlanIdOrderByPlannedActivitySessionOrder(
            Long athleteId, Long weeklyPlanId);
}
