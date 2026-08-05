package com.aicoach.backend.repository;

import com.aicoach.backend.models.GlobalPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GlobalPlanRepo extends JpaRepository<GlobalPlan, Long> {
    List<GlobalPlan> findByAthleteId(Long athleteId);
}
