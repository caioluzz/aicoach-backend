package com.aicoach.backend.models;

import jakarta.persistence.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workout_blocks")
@Data
public class WorkoutBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planned_activity_id", nullable = false)
    private PlannedActivity plannedActivity;

    // Ordem do bloco no treino (1: Aquecimento, 2: Principal, 3: Desaquecimento)
    @Column(name = "block_order", nullable = false)
    private Integer blockOrder;

    // O "pulo do gato": Quantas vezes este bloco se repete? (Padrão: 1)
    @Column(name = "iterations", nullable = false)
    private Integer iterations = 1;

    // Os passos que compõem este bloco
    @OneToMany(mappedBy = "workoutBlock", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkoutStep> steps = new ArrayList<>();
}