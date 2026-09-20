package com.aicoach.backend.models;

import com.aicoach.backend.enums.ActivityMatchType;
import com.aicoach.backend.enums.ComplianceClassification;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "activity_comparisons")
@Data
public class ActivityComparison {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "activity_id", unique = true)
    private Activity activity;

    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "planned_activity_id", unique = true)
    private PlannedActivity plannedActivity;

    @Enumerated(EnumType.STRING) @Column(name = "match_type", nullable = false, length = 20)
    private ActivityMatchType matchType;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ComplianceClassification classification;

    @Column(name = "compliance_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal compliancePercentage;

    @Column(name = "tolerance_version", nullable = false, length = 50)
    private String toleranceVersion;

    @Column(name = "planned_duration_s") private Integer plannedDurationSeconds;
    @Column(name = "actual_duration_s", precision = 10, scale = 2) private BigDecimal actualDurationSeconds;
    @Column(name = "planned_distance_m") private Integer plannedDistanceMeters;
    @Column(name = "actual_distance_m", precision = 12, scale = 2) private BigDecimal actualDistanceMeters;
    @Column(name = "actual_pace_s_per_km", precision = 8, scale = 2) private BigDecimal actualPaceSecondsPerKm;
    @Column(name = "actual_avg_hr", precision = 6, scale = 2) private BigDecimal actualAverageHeartRate;
    @Column(name = "actual_avg_cadence", precision = 6, scale = 2) private BigDecimal actualAverageCadence;
    @Column(nullable = false, length = 2000) private String explanation;
    @Column(name = "calculated_at", nullable = false) private Instant calculatedAt;

    @OneToMany(mappedBy = "comparison", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNumber ASC")
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private List<ActivityStepComparison> steps = new ArrayList<>();
}
