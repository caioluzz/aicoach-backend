package com.aicoach.backend.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "global_plans")
@Data
public class GlobalPlan {

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

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_weeks", nullable = false)
    private Integer totalWeeks;

    // Relacionamento 1:N com CicloTreino.
    // mappedBy: indica que o lado "dono" da relação é o atributo "globalPlan" na classe TrainingCycle.
    // orphanRemoval = true: se você tirar um ciclo da lista no Java, o Hibernate deleta do banco.
    @OneToMany(mappedBy = "globalPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrainingCycle> trainingCycles = new ArrayList<>();
}