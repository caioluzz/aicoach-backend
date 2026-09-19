package com.aicoach.backend.weeklyplan;

import com.aicoach.backend.enums.DurationType;
import com.aicoach.backend.enums.WorkoutType;
import com.aicoach.backend.training.daniels.DanielsIntensity;
import com.aicoach.backend.training.workout.WorkoutStepDefinition;

import java.time.LocalDate;
import java.util.List;

public record WeeklyPlanProposal(String summary, List<Session> sessions) {
    public record Session(Integer order, String name, LocalDate scheduledDate, WorkoutType workoutType,
                          List<Block> blocks) {}
    public record Block(Integer repetitions, List<Step> steps) {}
    public record Step(WorkoutStepDefinition.Kind kind, DurationType durationType, Integer durationValue,
                       DanielsIntensity intensity, String instruction) {}
}
