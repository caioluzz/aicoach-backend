package com.aicoach.backend.repository;

import com.aicoach.backend.models.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ActivityRepo extends JpaRepository<Activity, Long> {

    boolean existsByGarminActivityId(Long garminActivityId);

    long countByAthleteId(Long athleteId);

    Optional<Activity> findByAthleteIdAndIsVdotTestTrue(Long athleteId);

    Optional<Activity> findByGarminActivityId(Long garminActivityId);

    Optional<Activity> findByIdAndAthleteId(Long id, Long athleteId);
}
