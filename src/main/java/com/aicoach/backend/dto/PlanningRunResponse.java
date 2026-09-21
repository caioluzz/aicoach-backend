package com.aicoach.backend.dto;

import java.time.LocalDate;

public record PlanningRunResponse(
        Long athleteId,
        String status,
        boolean reused,
        String proposalType,
        LocalDate periodStart,
        LocalDate periodEnd,
        Integer activitiesConsidered,
        Integer feedbacksProcessed,
        Long weeklyPlanId,
        ActivitySyncResponse synchronization,
        String message) {
}
