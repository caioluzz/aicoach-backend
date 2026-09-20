package com.aicoach.backend.models;

import com.aicoach.backend.enums.ComplianceClassification;
import com.aicoach.backend.enums.StepAlignmentSource;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

@Entity
@Table(name = "activity_step_comparisons")
@Data
public class ActivityStepComparison {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "comparison_id", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private ActivityComparison comparison;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "workout_step_id", nullable = false)
    private WorkoutStep workoutStep;

    @Column(name = "sequence_number", nullable = false) private Integer sequenceNumber;
    @Column(name = "occurrence_index", nullable = false) private Integer occurrenceIndex;
    @Enumerated(EnumType.STRING) @Column(name = "alignment_source", nullable = false, length = 20)
    private StepAlignmentSource alignmentSource;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ComplianceClassification classification;
    @Column(name = "compliance_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal compliancePercentage;
    @Column(name = "actual_duration_s", precision = 10, scale = 2) private BigDecimal actualDurationSeconds;
    @Column(name = "actual_distance_m", precision = 12, scale = 2) private BigDecimal actualDistanceMeters;
    @Column(name = "actual_pace_s_per_km", precision = 8, scale = 2) private BigDecimal actualPaceSecondsPerKm;
    @Column(name = "actual_avg_hr", precision = 6, scale = 2) private BigDecimal actualAverageHeartRate;
    @Column(name = "actual_avg_cadence", precision = 6, scale = 2) private BigDecimal actualAverageCadence;
    @Column(name = "interval_start_s", precision = 10, scale = 2) private BigDecimal intervalStartSeconds;
    @Column(name = "interval_end_s", precision = 10, scale = 2) private BigDecimal intervalEndSeconds;
    @Column(name = "interval_start_m", precision = 12, scale = 2) private BigDecimal intervalStartMeters;
    @Column(name = "interval_end_m", precision = 12, scale = 2) private BigDecimal intervalEndMeters;
    @Column(nullable = false, length = 1000) private String explanation;
}
