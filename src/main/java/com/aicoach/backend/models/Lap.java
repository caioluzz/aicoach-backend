package com.aicoach.backend.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "laps")
@Data
public class Lap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Activity activity;

    @Column(name = "lap_number", nullable = false)
    private Short lapNumber;

    @Column(name = "lap_type", length = 30)
    private String lapType;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "duration_s", precision = 7, scale = 3)
    private BigDecimal durationS;

    @Column(name = "distance_km", precision = 6, scale = 3)
    private BigDecimal distanceKm;

    @Column(name = "avg_pace_s_per_km")
    private Short avgPaceSPerKm;

    @Column(name = "avg_speed_kmh", precision = 5, scale = 2)
    private BigDecimal avgSpeedKmh;

    @Column(name = "avg_hr")
    private Short avgHr;

    @Column(name = "max_hr")
    private Short maxHr;

    @Column(name = "avg_cadence")
    private Short avgCadence;

    @Column(name = "max_cadence")
    private Short maxCadence;

    @Column(name = "ascent_m")
    private Short ascentM;

    @Column(name = "descent_m")
    private Short descentM;
}
