package com.aicoach.backend.models;

import com.aicoach.backend.enums.HealthIssueStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "assessment_health_issues")
@Getter
@Setter
public class AssessmentHealthIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    private AthleteAssessment assessment;

    @Column(name = "body_area", nullable = false, length = 100)
    private String bodyArea;

    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HealthIssueStatus status;

    @Column(name = "pain_severity", nullable = false)
    private Integer painSeverity;

    @Column(name = "started_on")
    private LocalDate startedOn;

    @Column(name = "restriction_notes", length = 500)
    private String restrictionNotes;
}
