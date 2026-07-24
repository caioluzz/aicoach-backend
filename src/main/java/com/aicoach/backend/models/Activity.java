package com.aicoach.backend.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "activities")
@Data
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "garmin_activity_id", unique = true)
    private Long garminActivityId;

    private String name;

    @Column(name = "distance_meters")
    private Double distanceMeters;

    @Column(name = "duration_seconds")
    private Double durationSeconds;

    @Column(name = "average_heart_rate")
    private Integer averageHeartRate;

    @Column(name = "average_speed")
    private Double averageSpeed;

    @Column(name = "is_vdot_test", nullable = false)
    private Boolean isVdotTest = false;

    @Column(nullable = false, length = 30)
    private String sport;

    @Column(name = "sub_sport", length = 30)
    private String subSport;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "max_speed_kmh", precision = 5, scale = 2)
    private BigDecimal maxSpeedKmh;

    @Column(name = "avg_pace_s_per_km")
    private Short avgPaceSPerKm;

    @Column(name = "best_pace_s_per_km")
    private Short bestPaceSPerKm;

    @Column(name = "max_hr")
    private Short maxHr;

    @Column(name = "avg_cadence")
    private Short avgCadence;

    @Column(name = "max_cadence")
    private Short maxCadence;

    @Column(name = "elevation_gain_m")
    private Short elevationGainM;

    @Column(name = "elevation_loss_m")
    private Short elevationLossM;

    @Column(name = "min_altitude_m", precision = 5, scale = 1)
    private BigDecimal minAltitudeM;

    @Column(name = "max_altitude_m", precision = 5, scale = 1)
    private BigDecimal maxAltitudeM;

    @Column(name = "lap_count")
    private Short lapCount;

    @Column(name = "record_count")
    private Short recordCount;

    @Column(name = "raw_file_path")
    private String rawFilePath;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Lap> laps = new ArrayList<>();

    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ActivityRecord> records = new ArrayList<>();
}