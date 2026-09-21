package com.aicoach.backend.models;

import com.aicoach.backend.enums.FeedbackFeeling;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "athlete_feedback")
@Getter
@Setter
public class AthleteFeedback {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private Activity activity;

    @Column(name = "feedback_date", nullable = false)
    private LocalDate feedbackDate;

    @Column(name = "perceived_effort", nullable = false)
    private Integer perceivedEffort;

    @Column(nullable = false)
    private Integer fatigue;

    @Column(name = "sleep_hours", nullable = false)
    private Double sleepHours;

    @Column(name = "pain_severity", nullable = false)
    private Integer painSeverity;

    @Column(name = "pain_location", length = 200)
    private String painLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackFeeling feeling;

    @Column(length = 2000)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
