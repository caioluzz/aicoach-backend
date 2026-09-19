package com.aicoach.backend.repository;

import com.aicoach.backend.enums.WeeklyPlanStatus;
import com.aicoach.backend.models.WeeklyPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WeeklyPlanRepo extends JpaRepository<WeeklyPlan, Long> {
    Optional<WeeklyPlan> findByIdAndAthleteId(Long id, Long athleteId);
    Optional<WeeklyPlan> findTopByAthleteIdAndGlobalPlanIdAndSeasonPlanWeekWeekNumberOrderByVersionDesc(
            Long athleteId, Long globalPlanId, Integer weekNumber);
    List<WeeklyPlan> findByAthleteIdAndGlobalPlanIdAndSeasonPlanWeekWeekNumberOrderByVersionDesc(
            Long athleteId, Long globalPlanId, Integer weekNumber);
    Optional<WeeklyPlan> findFirstByAthleteIdAndSeasonPlanWeekIdAndStatus(
            Long athleteId, Long seasonPlanWeekId, WeeklyPlanStatus status);
}
