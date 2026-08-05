package com.aicoach.backend.repository;

import com.aicoach.backend.models.TrainingCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainingCycleRepo extends JpaRepository<TrainingCycle, Long> {
    List<TrainingCycle> findByGlobalPlanId(Long globalPlanId);
}