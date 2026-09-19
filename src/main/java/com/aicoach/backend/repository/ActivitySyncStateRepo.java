package com.aicoach.backend.repository;

import com.aicoach.backend.models.ActivitySyncState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActivitySyncStateRepo extends JpaRepository<ActivitySyncState, Long> {
    Optional<ActivitySyncState> findByAthleteId(Long athleteId);
}
