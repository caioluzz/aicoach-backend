package com.aicoach.backend.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

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
}