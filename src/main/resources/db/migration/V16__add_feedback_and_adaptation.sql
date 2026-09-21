CREATE TABLE athlete_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    athlete_id BIGINT NOT NULL,
    activity_id BIGINT,
    feedback_date DATE NOT NULL,
    perceived_effort INT NOT NULL,
    fatigue INT NOT NULL,
    sleep_hours DOUBLE NOT NULL,
    pain_severity INT NOT NULL,
    pain_location VARCHAR(200),
    feeling VARCHAR(20) NOT NULL,
    comment VARCHAR(2000),
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT ck_feedback_effort CHECK (perceived_effort BETWEEN 1 AND 10),
    CONSTRAINT ck_feedback_fatigue CHECK (fatigue BETWEEN 1 AND 10),
    CONSTRAINT ck_feedback_sleep CHECK (sleep_hours BETWEEN 0 AND 24),
    CONSTRAINT ck_feedback_pain CHECK (pain_severity BETWEEN 0 AND 10),
    CONSTRAINT fk_feedback_athlete FOREIGN KEY (athlete_id) REFERENCES athletes(id),
    CONSTRAINT fk_feedback_activity FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE SET NULL
);

CREATE INDEX idx_feedback_athlete_date ON athlete_feedback (athlete_id, feedback_date);

CREATE TABLE adaptation_decisions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    athlete_id BIGINT NOT NULL,
    feedback_id BIGINT NOT NULL,
    comparison_id BIGINT,
    rule_version VARCHAR(50) NOT NULL,
    alert_level VARCHAR(30) NOT NULL,
    load_reduction_percent INT NOT NULL,
    allow_intensity BOOLEAN NOT NULL,
    weekly_review_required BOOLEAN NOT NULL,
    season_plan_review_proposed BOOLEAN NOT NULL,
    material_cause VARCHAR(500),
    rationale VARCHAR(2000) NOT NULL,
    evidence_snapshot VARCHAR(2000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_adaptation_feedback UNIQUE (feedback_id),
    CONSTRAINT ck_adaptation_reduction CHECK (load_reduction_percent BETWEEN 0 AND 100),
    CONSTRAINT fk_adaptation_athlete FOREIGN KEY (athlete_id) REFERENCES athletes(id),
    CONSTRAINT fk_adaptation_feedback FOREIGN KEY (feedback_id) REFERENCES athlete_feedback(id) ON DELETE CASCADE,
    CONSTRAINT fk_adaptation_comparison FOREIGN KEY (comparison_id) REFERENCES activity_comparisons(id) ON DELETE SET NULL
);

CREATE INDEX idx_adaptation_athlete_created ON adaptation_decisions (athlete_id, created_at);

CREATE TABLE workout_adjustments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    decision_id BIGINT NOT NULL,
    planned_activity_id BIGINT NOT NULL,
    scheduled_date DATE NOT NULL,
    action VARCHAR(30) NOT NULL,
    original_distance_m INT,
    proposed_distance_m INT,
    original_duration_s INT,
    proposed_duration_s INT,
    original_stress_points INT,
    proposed_stress_points INT,
    rationale VARCHAR(500) NOT NULL,
    CONSTRAINT uk_adjustment_decision_activity UNIQUE (decision_id, planned_activity_id),
    CONSTRAINT fk_adjustment_decision FOREIGN KEY (decision_id) REFERENCES adaptation_decisions(id) ON DELETE CASCADE,
    CONSTRAINT fk_adjustment_planned FOREIGN KEY (planned_activity_id) REFERENCES planned_activities(id)
);
