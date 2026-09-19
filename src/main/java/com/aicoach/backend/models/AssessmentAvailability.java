package com.aicoach.backend.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;

@Entity
@Table(name = "assessment_availability", uniqueConstraints =
        @UniqueConstraint(name = "uk_assessment_availability_day", columnNames = {"assessment_id", "day_of_week"}))
@Getter
@Setter
public class AssessmentAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    private AthleteAssessment assessment;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 20)
    private DayOfWeek dayOfWeek;

    @Column(name = "available_minutes", nullable = false)
    private Integer availableMinutes;
}
