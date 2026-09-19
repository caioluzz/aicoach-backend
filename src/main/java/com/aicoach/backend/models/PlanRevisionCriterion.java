package com.aicoach.backend.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "plan_revision_criteria", uniqueConstraints =
        @UniqueConstraint(name = "uk_plan_revision_code", columnNames = {"global_plan_id", "code"}))
@Getter
@Setter
public class PlanRevisionCriterion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "global_plan_id", nullable = false)
    private GlobalPlan globalPlan;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 1000)
    private String description;
}
