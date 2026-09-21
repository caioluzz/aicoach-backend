package com.aicoach.backend.repository;

import com.aicoach.backend.models.WorkoutAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutAdjustmentRepo extends JpaRepository<WorkoutAdjustment, Long> {
}
