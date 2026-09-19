package com.aicoach.backend.comparison;

/** Versioned deterministic inputs to the compliance calculation. */
public record ComparisonTolerancePolicy(
        String version,
        double durationToleranceRatio,
        double distanceToleranceRatio,
        double paceToleranceRatio,
        double fallbackPaceSecondsPerKm,
        double exceededRatio,
        double fulfilledMinimum,
        double partialMinimum) {

    public static final ComparisonTolerancePolicy V1 = new ComparisonTolerancePolicy(
            "activity-comparison-v1", 0.10, 0.10, 0.05, 360.0, 0.15, 90.0, 60.0);
}
