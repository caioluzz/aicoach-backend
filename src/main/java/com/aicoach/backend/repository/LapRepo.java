package com.aicoach.backend.repository;

import com.aicoach.backend.models.Lap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LapRepo extends JpaRepository<Lap, Long> {
}
