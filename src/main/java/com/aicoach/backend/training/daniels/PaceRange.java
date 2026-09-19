package com.aicoach.backend.training.daniels;

/** Pace boundaries in whole seconds per kilometre. */
public record PaceRange(int fastestSecondsPerKm, int slowestSecondsPerKm) {

    public PaceRange {
        if (fastestSecondsPerKm <= 0 || slowestSecondsPerKm <= 0) {
            throw new IllegalArgumentException("Pace boundaries must be positive");
        }
        if (fastestSecondsPerKm > slowestSecondsPerKm) {
            throw new IllegalArgumentException("Fastest pace cannot be slower than slowest pace");
        }
    }
}
