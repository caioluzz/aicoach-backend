package com.aicoach.backend.weeklyplan;

import com.aicoach.backend.training.daniels.DanielsIntensity;

import java.util.List;

public record WeeklyPlanCalculation(int totalDistanceMeters, int totalDurationSeconds,
                                    List<Session> sessions) {
    public record Session(int order, int distanceMeters, int durationSeconds,
                          List<IntensityLoad> intensityLoads) {}
    public record IntensityLoad(DanielsIntensity intensity, int distanceMeters, int durationSeconds) {}
}
