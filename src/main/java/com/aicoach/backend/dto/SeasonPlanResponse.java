package com.aicoach.backend.dto;

import com.aicoach.backend.enums.SeasonPlanStatus;
import com.aicoach.backend.enums.TrainingPhase;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SeasonPlanResponse(
        Long id, Long athleteId, Long objectiveId, Long assessmentId, Integer assessmentVersion,
        Long performanceProfileId, Integer version, SeasonPlanStatus status,
        LocalDate startDate, LocalDate endDate, Integer totalWeeks, String summary,
        Instant createdAt, Instant reviewedAt, String reviewComment,
        Generation generation, List<Phase> phases, List<Week> weeks,
        List<RevisionCriterion> revisionCriteria
) {
    public record Generation(String source, String responseId, String model, String promptVersion,
                             String schemaVersion, Integer inputTokens, Integer outputTokens,
                             Long latencyMs) {}
    public record Phase(Integer order, TrainingPhase phase, Integer startWeek, Integer endWeek,
                        LocalDate startDate, LocalDate endDate, String objective,
                        String expectedProgression, Double maxWeeklyVolumeKm) {}
    public record Week(Integer weekNumber, LocalDate startDate, LocalDate endDate,
                       Double targetVolumeKm, String focus, Boolean recoveryWeek, Boolean taperWeek) {}
    public record RevisionCriterion(String code, String description) {}
}
