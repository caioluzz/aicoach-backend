package com.aicoach.backend.models;

import com.aicoach.backend.enums.ObjectiveStatus;
import com.aicoach.backend.enums.RacePriority;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "objectives")
@Data
public class Objective {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    @Column(nullable = false)
    private String title;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate; // Ex: 2026-08-02

    @Column(name = "target_distance_m", nullable = false)
    private Integer targetDistance_m;

    @Column(name = "target_time_minutes")
    private Integer targetTimeMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private RacePriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ObjectiveStatus status;
}