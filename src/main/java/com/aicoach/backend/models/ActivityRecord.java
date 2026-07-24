package com.aicoach.backend.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

@Entity
@Table(name = "activity_records")
@Data
public class ActivityRecord {

    @EmbeddedId
    private ActivityRecordId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("activityId")
    @JoinColumn(name = "activity_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Activity activity;

    @Column(name = "elapsed_s")
    private Short elapsedS;

    @Column(name = "distance_km", precision = 6, scale = 3)
    private BigDecimal distanceKm;

    @Column(name = "speed_kmh", precision = 5, scale = 2)
    private BigDecimal speedKmh;

    @Column(name = "pace_s_per_km")
    private Short paceSPerKm;

    @Column(name = "heart_rate")
    private Short heartRate;

    @Column(name = "cadence")
    private Short cadence;

    @Column(name = "altitude_m", precision = 5, scale = 1)
    private BigDecimal altitudeM;
}
