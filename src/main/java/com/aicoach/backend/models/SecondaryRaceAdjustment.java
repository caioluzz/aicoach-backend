package com.aicoach.backend.models;

import com.aicoach.backend.enums.RaceAdjustmentWindow;
import com.aicoach.backend.enums.SecondaryRaceAdjustmentAction;
import com.aicoach.backend.enums.WorkoutType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "secondary_race_adjustments")
@Getter
@Setter
public class SecondaryRaceAdjustment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "secondary_race_id", nullable = false)
    private SecondaryRace secondaryRace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "weekly_plan_id")
    private WeeklyPlan weeklyPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planned_activity_id")
    private PlannedActivity plannedActivity;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_window", nullable = false, length = 20)
    private RaceAdjustmentWindow adjustmentWindow;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SecondaryRaceAdjustmentAction action;

    @Column(name = "original_date")
    private LocalDate originalDate;

    @Column(name = "proposed_date", nullable = false)
    private LocalDate proposedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "original_workout_type", length = 30)
    private WorkoutType originalWorkoutType;

    @Enumerated(EnumType.STRING)
    @Column(name = "proposed_workout_type", nullable = false, length = 30)
    private WorkoutType proposedWorkoutType;

    @Column(name = "original_distance_m")
    private Integer originalDistanceMeters;

    @Column(name = "proposed_distance_m")
    private Integer proposedDistanceMeters;

    @Column(name = "original_duration_s")
    private Integer originalDurationSeconds;

    @Column(name = "proposed_duration_s")
    private Integer proposedDurationSeconds;

    @Column(name = "load_percent", nullable = false)
    private Integer loadPercent;

    @Column(nullable = false, length = 500)
    private String rationale;
}
