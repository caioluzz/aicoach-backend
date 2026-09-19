package com.aicoach.backend.seasonplan;

import com.aicoach.backend.enums.TrainingPhase;

import java.time.LocalDate;
import java.util.List;

public record SeasonPlanProposal(
        String summary,
        List<Phase> phases,
        List<Week> weeks,
        List<RevisionCriterion> revisionCriteria
) {
    public record Phase(Integer order, TrainingPhase phase, Integer startWeek, Integer endWeek,
                        String objective, String expectedProgression) {}

    public record Week(Integer weekNumber, LocalDate startDate, LocalDate endDate,
                       Double targetVolumeKm, String focus, Boolean recoveryWeek, Boolean taperWeek) {}

    public record RevisionCriterion(String code, String description) {}
}
