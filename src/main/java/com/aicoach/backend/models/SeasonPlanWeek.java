package com.aicoach.backend.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "season_plan_weeks", uniqueConstraints =
        @UniqueConstraint(name = "uk_plan_week_number", columnNames = {"global_plan_id", "week_number"}))
@Getter
@Setter
public class SeasonPlanWeek {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "global_plan_id", nullable = false)
    private GlobalPlan globalPlan;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "target_volume_km", nullable = false)
    private Double targetVolumeKm;

    @Column(nullable = false, length = 500)
    private String focus;

    @Column(name = "recovery_week", nullable = false)
    private Boolean recoveryWeek;

    @Column(name = "taper_week", nullable = false)
    private Boolean taperWeek;
}
