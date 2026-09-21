package com.aicoach.backend.models;

import com.aicoach.backend.enums.AdaptationAlertLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "adaptation_decisions")
@Getter
@Setter
public class AdaptationDecision {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feedback_id", nullable = false, unique = true)
    private AthleteFeedback feedback;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comparison_id")
    private ActivityComparison comparison;

    @Column(name = "rule_version", nullable = false, length = 50)
    private String ruleVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_level", nullable = false, length = 30)
    private AdaptationAlertLevel alertLevel;

    @Column(name = "load_reduction_percent", nullable = false)
    private Integer loadReductionPercent;

    @Column(name = "allow_intensity", nullable = false)
    private boolean allowIntensity;

    @Column(name = "weekly_review_required", nullable = false)
    private boolean weeklyReviewRequired;

    @Column(name = "season_plan_review_proposed", nullable = false)
    private boolean seasonPlanReviewProposed;

    @Column(name = "material_cause", length = 500)
    private String materialCause;

    @Column(nullable = false, length = 2000)
    private String rationale;

    @Column(name = "evidence_snapshot", nullable = false, length = 2000)
    private String evidenceSnapshot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "decision", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("scheduledDate ASC, id ASC")
    private List<WorkoutAdjustment> workoutAdjustments = new ArrayList<>();
}
