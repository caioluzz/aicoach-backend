package com.aicoach.backend.training.validation;

import com.aicoach.backend.training.daniels.DanielsIntensity;

import java.time.LocalDate;
import java.util.List;

public record TrainingLoad(
        int weeklyDistanceMeters,
        int weeklyDurationSeconds,
        int priorWeeklyDistanceMeters,
        int priorWeeklyDurationSeconds,
        int stableWeeksAtPriorLoad,
        int sessionsPerWeek,
        List<SessionLoad> sessions) {

    public TrainingLoad {
        if (weeklyDistanceMeters < 0 || weeklyDurationSeconds < 0
                || priorWeeklyDistanceMeters < 0 || priorWeeklyDurationSeconds < 0
                || stableWeeksAtPriorLoad < 0 || sessionsPerWeek < 0 || sessions == null) {
            throw new IllegalArgumentException("Training load values cannot be negative or null");
        }
        sessions = List.copyOf(sessions);
    }

    public record SessionLoad(LocalDate date, DanielsIntensity intensity, int distanceMeters, int durationSeconds) {
        public SessionLoad {
            if (date == null || intensity == null || distanceMeters < 0 || durationSeconds < 0) {
                throw new IllegalArgumentException("Session load is invalid");
            }
        }
    }
}
