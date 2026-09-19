package com.aicoach.backend.repository;

import com.aicoach.backend.models.GlobalPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GlobalPlanRepo extends JpaRepository<GlobalPlan, Long> {
    List<GlobalPlan> findByAthleteIdOrderByCreatedAtDesc(Long athleteId);
    Optional<GlobalPlan> findByIdAndAthleteId(Long id, Long athleteId);
    Optional<GlobalPlan> findTopByAthleteIdAndObjectiveIdOrderByVersionDesc(Long athleteId, Long objectiveId);
    Optional<GlobalPlan> findTopByAthleteIdOrderByCreatedAtDesc(Long athleteId);
    Optional<GlobalPlan> findFirstByAthleteIdAndObjectiveIdAndStatus(
            Long athleteId, Long objectiveId, com.aicoach.backend.enums.SeasonPlanStatus status);
}
