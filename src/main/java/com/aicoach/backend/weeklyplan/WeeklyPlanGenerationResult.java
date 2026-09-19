package com.aicoach.backend.weeklyplan;

public record WeeklyPlanGenerationResult(
        WeeklyPlanProposal proposal,
        String responseId,
        String model,
        Integer inputTokens,
        Integer outputTokens,
        Long latencyMs) {
}
