package com.aicoach.backend.models;

import com.aicoach.backend.enums.WorkoutAdjustmentAction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "workout_adjustments", uniqueConstraints = @UniqueConstraint(
        name = "uk_adjustment_decision_activity", columnNames = {"decision_id", "planned_activity_id"}))
@Getter
@Setter
public class WorkoutAdjustment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "decision_id", nullable = false)
    private AdaptationDecision decision;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "planned_activity_id", nullable = false)
    private PlannedActivity plannedActivity;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkoutAdjustmentAction action;

    @Column(name = "original_distance_m")
    private Integer originalDistanceMeters;

    @Column(name = "proposed_distance_m")
    private Integer proposedDistanceMeters;

    @Column(name = "original_duration_s")
    private Integer originalDurationSeconds;

    @Column(name = "proposed_duration_s")
    private Integer proposedDurationSeconds;

    @Column(name = "original_stress_points")
    private Integer originalStressPoints;

    @Column(name = "proposed_stress_points")
    private Integer proposedStressPoints;

    @Column(nullable = false, length = 500)
    private String rationale;
}
