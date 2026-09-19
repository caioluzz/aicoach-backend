package com.aicoach.backend.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "weekly_plans", uniqueConstraints =
        @UniqueConstraint(name = "uk_weekly_plan_week_version",
                columnNames = {"season_plan_week_id", "version"}))
@Getter
@Setter
public class WeeklyPlan {
    public static final String CURRENT_PROMPT_VERSION = "weekly-plan-1.0";
    public static final String CURRENT_SCHEMA_VERSION = "1.0";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "global_plan_id", nullable = false)
    private GlobalPlan globalPlan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_plan_week_id", nullable = false)
    private SeasonPlanWeek seasonPlanWeek;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "training_cycle_id", nullable = false)
    private TrainingCycle trainingCycle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    private AthleteAssessment assessment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "athlete_metrics_id", nullable = false)
    private AthleteMetrics athleteMetrics;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "week_end", nullable = false)
    private LocalDate weekEnd;

    @Column(name = "target_volume_km", nullable = false)
    private Double targetVolumeKm;

    @Column(name = "planned_distance_m", nullable = false)
    private Integer plannedDistanceMeters;

    @Column(name = "planned_duration_s", nullable = false)
    private Integer plannedDurationSeconds;

    @Column(nullable = false, length = 2000)
    private String summary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "generation_source", nullable = false, length = 20)
    private String generationSource;

    @Column(name = "openai_response_id", length = 100)
    private String openaiResponseId;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "prompt_version", nullable = false, length = 40)
    private String promptVersion = CURRENT_PROMPT_VERSION;

    @Column(name = "schema_version", nullable = false, length = 20)
    private String schemaVersion = CURRENT_SCHEMA_VERSION;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "generation_latency_ms")
    private Long generationLatencyMs;

    @OneToMany(mappedBy = "weeklyPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sessionOrder ASC")
    private List<PlannedActivity> sessions = new ArrayList<>();

    @PrePersist
    void initializeCreatedAt() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
