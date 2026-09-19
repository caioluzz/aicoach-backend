package com.aicoach.backend.training.daniels;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.EnumMap;
import java.util.Map;

/** Deterministic implementation of the Daniels/Gilbert performance equations. */
public final class DanielsPerformanceEngine {

    public static final String ENGINE_VERSION = "daniels-2ed-ptbr-v1";
    public static final String COEFFICIENT_SET = "daniels-gilbert-vdot-2ed";
    public static final int TEST_DISTANCE_METERS = 3_000;
    public static final int DISTANCE_TOLERANCE_METERS = 30;

    private static final double OXYGEN_INTERCEPT = -4.60;
    private static final double OXYGEN_LINEAR = 0.182258;
    private static final double OXYGEN_QUADRATIC = 0.000104;
    private static final double FRACTION_BASE = 0.8;
    private static final double FRACTION_SLOW_AMPLITUDE = 0.1894393;
    private static final double FRACTION_SLOW_DECAY = -0.012778;
    private static final double FRACTION_FAST_AMPLITUDE = 0.2989558;
    private static final double FRACTION_FAST_DECAY = -0.1932605;

    private final Clock clock;

    public DanielsPerformanceEngine(Clock clock) {
        this.clock = clock;
    }

    public PerformanceProfile fromThreeKmTest(int measuredDistanceMeters, int durationSeconds) {
        if (Math.abs(measuredDistanceMeters - TEST_DISTANCE_METERS) > DISTANCE_TOLERANCE_METERS) {
            throw new IllegalArgumentException("3 km test distance must be between 2970 and 3030 metres");
        }
        if (durationSeconds < 210 || durationSeconds > 3_600) {
            throw new IllegalArgumentException("3 km test duration must be between 3:30 and 60:00");
        }

        // Normalize small GPS variation to the prescribed 3,000 m protocol.
        double durationMinutes = durationSeconds / 60.0;
        double velocityMetersPerMinute = TEST_DISTANCE_METERS / durationMinutes;
        double oxygenCost = OXYGEN_INTERCEPT
                + OXYGEN_LINEAR * velocityMetersPerMinute
                + OXYGEN_QUADRATIC * velocityMetersPerMinute * velocityMetersPerMinute;
        double sustainableFraction = FRACTION_BASE
                + FRACTION_SLOW_AMPLITUDE * Math.exp(FRACTION_SLOW_DECAY * durationMinutes)
                + FRACTION_FAST_AMPLITUDE * Math.exp(FRACTION_FAST_DECAY * durationMinutes);
        double vdot = round(oxygenCost / sustainableFraction, 2);

        if (vdot < 20.0 || vdot > 85.0) {
            throw new IllegalArgumentException("Calculated VDOT is outside the supported range 20.00-85.00");
        }

        return new PerformanceProfile(
                vdot,
                measuredDistanceMeters,
                durationSeconds,
                clock.instant(),
                ENGINE_VERSION,
                COEFFICIENT_SET,
                paceRanges(vdot));
    }

    private static Map<DanielsIntensity, PaceRange> paceRanges(double vdot) {
        EnumMap<DanielsIntensity, PaceRange> result = new EnumMap<>(DanielsIntensity.class);
        result.put(DanielsIntensity.E, rangeForFractions(vdot, 0.59, 0.74));
        result.put(DanielsIntensity.M, rangeForFractions(vdot, 0.75, 0.84));
        result.put(DanielsIntensity.T, rangeForFractions(vdot, 0.83, 0.88));
        result.put(DanielsIntensity.I, rangeForFractions(vdot, 0.95, 1.00));

        PaceRange interval = result.get(DanielsIntensity.I);
        // Daniels' rule for distance runners: R is about 6 s/400 m (15 s/km) faster than I.
        result.put(DanielsIntensity.R, new PaceRange(
                interval.fastestSecondsPerKm() - 15,
                interval.slowestSecondsPerKm() - 15));
        return result;
    }

    private static PaceRange rangeForFractions(double vdot, double lowFraction, double highFraction) {
        int slow = secondsPerKmForOxygenCost(vdot * lowFraction);
        int fast = secondsPerKmForOxygenCost(vdot * highFraction);
        return new PaceRange(fast, slow);
    }

    private static int secondsPerKmForOxygenCost(double oxygenCost) {
        double c = OXYGEN_INTERCEPT - oxygenCost;
        double discriminant = OXYGEN_LINEAR * OXYGEN_LINEAR - 4 * OXYGEN_QUADRATIC * c;
        double velocityMetersPerMinute = (-OXYGEN_LINEAR + Math.sqrt(discriminant)) / (2 * OXYGEN_QUADRATIC);
        return (int) Math.round(60_000.0 / velocityMetersPerMinute);
    }

    private static double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }
}
