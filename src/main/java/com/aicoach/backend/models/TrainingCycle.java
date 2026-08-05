package com.aicoach.backend.models;

import com.aicoach.backend.enums.TrainingPhase;
import jakarta.persistence.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "training_cycles")
@Data
public class TrainingCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relacionamento com o Plano Global
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "global_plan_id", nullable = false)
    private GlobalPlan globalPlan;

    // A fase metodológica de Jack Daniels
    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false)
    private TrainingPhase phase;

    // O volume máximo que o atleta pode atingir nesta fase específica
    @Column(name = "max_weekly_volume_km", nullable = false)
    private Double maxWeeklyVolumeKm;

    // O teto de pontos de estresse (carga de intensidade) para controlar o overtraining
    @Column(name = "max_stress_points")
    private Integer maxStressPoints;

    // Relacionamento 1:N com as atividades (microciclos/sessões)
    @OneToMany(mappedBy = "trainingCycle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlannedActivity> plannedActivities = new ArrayList<>();
}