package com.aicoach.backend.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "athlete_metrics")
@Data
public class AthleteMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt; // Data e hora exata em que a IA gerou esta métrica

    @Column(nullable = false)
    private Double vdot; // Ex: 45.2

    @Column(name = "easy_pace_sec")
    private Integer easyPaceSec; // E Pace (Easy / Longo)

    @Column(name = "marathon_pace_sec")
    private Integer marathonPaceSec; // M Pace (Maratona)

    @Column(name = "threshold_pace_sec")
    private Integer thresholdPaceSec; // T Pace (Limiar / Tempo Run)

    @Column(name = "interval_pace_sec")
    private Integer intervalPaceSec; // I Pace (Intervalado de VO2Max)

    @Column(name = "repetition_pace_sec")
    private Integer repetitionPaceSec; // R Pace (Repetição / Economia de corrida)
}