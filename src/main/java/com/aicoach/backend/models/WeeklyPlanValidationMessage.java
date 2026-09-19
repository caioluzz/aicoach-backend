package com.aicoach.backend.models;

import com.aicoach.backend.enums.ValidationSeverity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "weekly_plan_validation_messages", uniqueConstraints =
        @UniqueConstraint(name = "uk_weekly_plan_validation_code", columnNames = {"weekly_plan_id", "code"}))
@Getter
@Setter
public class WeeklyPlanValidationMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "weekly_plan_id", nullable = false)
    private WeeklyPlan weeklyPlan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ValidationSeverity severity;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 500)
    private String message;
}
