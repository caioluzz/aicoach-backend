package com.aicoach.backend.models;

import com.aicoach.backend.enums.WorkoutType;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "planned_activities")
@Data
public class PlannedActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relacionamento com o Ciclo (Mesociclo)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_cycle_id", nullable = false)
    private TrainingCycle trainingCycle;

    // Data em que o treino deve ser executado
    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    // Tipo do treino na visão macro da semana
    @Enumerated(EnumType.STRING)
    @Column(name = "workout_type", nullable = false)
    private WorkoutType workoutType;

    // Carga calculada desta sessão (para somar no limite do ciclo e validar no LangGraph)
    @Column(name = "calculated_stress_points")
    private Integer calculatedStressPoints;

    // Relacionamento 1:N com os Passos do Treino (os blocos que vão para o relógio Garmin)
    @OneToMany(mappedBy = "plannedActivity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkoutBlock> workoutBlocks = new ArrayList<>();
}