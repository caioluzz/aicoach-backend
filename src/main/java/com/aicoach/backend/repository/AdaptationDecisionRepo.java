package com.aicoach.backend.repository;

import com.aicoach.backend.enums.AdaptationAlertLevel;
import com.aicoach.backend.models.AdaptationDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AdaptationDecisionRepo extends JpaRepository<AdaptationDecision, Long> {
    List<AdaptationDecision> findByAthleteIdOrderByCreatedAtDesc(Long athleteId);

    Optional<AdaptationDecision> findTopByAthleteIdOrderByCreatedAtDesc(Long athleteId);

    long countByAthleteIdAndAlertLevelInAndCreatedAtGreaterThanEqual(
            Long athleteId, Collection<AdaptationAlertLevel> alertLevels, Instant since);
}
