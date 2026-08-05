package com.aicoach.backend.models;

import com.aicoach.backend.enums.DurationType;
import com.aicoach.backend.enums.IntensityZone;
import com.aicoach.backend.enums.StepType;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "workout_steps")
@Data
public class WorkoutStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relacionamento com a Atividade (Sessão do dia)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planned_activity_id", nullable = false)
    private PlannedActivity plannedActivity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_block_id", nullable = false)
    private WorkoutBlock workoutBlock;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    // Tipo do passo (Aquecimento, Trabalho, Recuperação, Desaquecimento)
    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false)
    private StepType stepType;

    // Define se a duração é baseada em TEMPO (segundos) ou DISTÂNCIA (metros)
    @Enumerated(EnumType.STRING)
    @Column(name = "duration_type", nullable = false)
    private DurationType durationType;

    // O valor da duração (ex: 400 para metros, ou 120 para segundos)
    @Column(name = "duration_value", nullable = false)
    private Integer durationValue;

    // Zona de intensidade alvo (E, M, T, I, R) baseada na metodologia Daniels
    @Enumerated(EnumType.STRING)
    @Column(name = "target_zone", nullable = false)
    private IntensityZone targetZone;
}