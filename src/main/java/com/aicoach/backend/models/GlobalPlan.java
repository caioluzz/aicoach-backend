package com.aicoach.backend.models;

import com.aicoach.backend.enums.SeasonPlanStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "global_plans")
@Getter
@Setter
public class GlobalPlan {

    public static final String CURRENT_PROMPT_VERSION = "season-plan-1.0";
    public static final String CURRENT_SCHEMA_VERSION = "1.0";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relacionamento com o Atleta
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    // Relacionamento com a Prova Alvo (Objetivo)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "objective_id", nullable = false)
    private Objective objective;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    private AthleteAssessment assessment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "athlete_metrics_id", nullable = false)
    private AthleteMetrics athleteMetrics;

    @Column(nullable = false)
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeasonPlanStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_weeks", nullable = false)
    private Integer totalWeeks;

    @Column(nullable = false, length = 2000)
    private String summary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_comment", length = 1000)
    private String reviewComment;

    @Column(name = "generation_source", nullable = false, length = 20)
    private String generationSource;

    @Column(name = "openai_response_id", length = 100)
    private String openaiResponseId;

    @Column(name = "model", nullable = false, length = 100)
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

    // Relacionamento 1:N com CicloTreino.
    // mappedBy: indica que o lado "dono" da relação é o atributo "globalPlan" na classe TrainingCycle.
    // orphanRemoval = true: se você tirar um ciclo da lista no Java, o Hibernate deleta do banco.
    @OneToMany(mappedBy = "globalPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrainingCycle> trainingCycles = new ArrayList<>();

    @OneToMany(mappedBy = "globalPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("weekNumber ASC")
    private List<SeasonPlanWeek> weeks = new ArrayList<>();

    @OneToMany(mappedBy = "globalPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("code ASC")
    private List<PlanRevisionCriterion> revisionCriteria = new ArrayList<>();

    @PrePersist
    void initializeCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
