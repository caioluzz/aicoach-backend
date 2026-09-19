package com.aicoach.backend.training.daniels;

import java.time.Instant;
import java.util.Map;

public record PerformanceProfile(
        double vdot,
        int testDistanceMeters,
        int testDurationSeconds,
        Instant calculatedAt,
        String engineVersion,
        String coefficientSet,
        Map<DanielsIntensity, PaceRange> paces) {

    public PerformanceProfile {
        paces = Map.copyOf(paces);
    }
}
