package com.aicoach.backend.seasonplan;

public record SeasonPlanGenerationResult(
        SeasonPlanProposal proposal,
        String responseId,
        String model,
        Integer inputTokens,
        Integer outputTokens,
        Long latencyMs
) {}
