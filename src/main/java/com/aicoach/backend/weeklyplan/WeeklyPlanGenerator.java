package com.aicoach.backend.weeklyplan;

public interface WeeklyPlanGenerator {
    WeeklyPlanGenerationResult generate(WeeklyPlanGenerationContext context);
}
