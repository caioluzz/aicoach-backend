CREATE TABLE activity_comparisons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    athlete_id BIGINT NOT NULL,
    activity_id BIGINT,
    planned_activity_id BIGINT,
    match_type VARCHAR(20) NOT NULL,
    classification VARCHAR(20) NOT NULL,
    compliance_percentage DECIMAL(5,2) NOT NULL,
    tolerance_version VARCHAR(50) NOT NULL,
    planned_duration_s INT,
    actual_duration_s DECIMAL(10,2),
    planned_distance_m INT,
    actual_distance_m DECIMAL(12,2),
    actual_pace_s_per_km DECIMAL(8,2),
    actual_avg_hr DECIMAL(6,2),
    actual_avg_cadence DECIMAL(6,2),
    explanation VARCHAR(2000) NOT NULL,
    calculated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_activity_comparison_activity UNIQUE (activity_id),
    CONSTRAINT uk_activity_comparison_planned UNIQUE (planned_activity_id),
    CONSTRAINT ck_activity_comparison_reference CHECK (activity_id IS NOT NULL OR planned_activity_id IS NOT NULL),
    CONSTRAINT fk_activity_comparison_athlete FOREIGN KEY (athlete_id) REFERENCES athletes(id),
    CONSTRAINT fk_activity_comparison_activity FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_comparison_planned FOREIGN KEY (planned_activity_id) REFERENCES planned_activities(id) ON DELETE CASCADE
);

CREATE INDEX idx_activity_comparison_athlete_calculated
    ON activity_comparisons (athlete_id, calculated_at);

CREATE TABLE activity_step_comparisons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comparison_id BIGINT NOT NULL,
    workout_step_id BIGINT NOT NULL,
    sequence_number INT NOT NULL,
    occurrence_index INT NOT NULL,
    alignment_source VARCHAR(20) NOT NULL,
    classification VARCHAR(20) NOT NULL,
    compliance_percentage DECIMAL(5,2) NOT NULL,
    actual_duration_s DECIMAL(10,2),
    actual_distance_m DECIMAL(12,2),
    actual_pace_s_per_km DECIMAL(8,2),
    actual_avg_hr DECIMAL(6,2),
    actual_avg_cadence DECIMAL(6,2),
    interval_start_s DECIMAL(10,2),
    interval_end_s DECIMAL(10,2),
    interval_start_m DECIMAL(12,2),
    interval_end_m DECIMAL(12,2),
    explanation VARCHAR(1000) NOT NULL,
    CONSTRAINT uk_activity_step_comparison_sequence UNIQUE (comparison_id, sequence_number),
    CONSTRAINT fk_activity_step_comparison_parent FOREIGN KEY (comparison_id) REFERENCES activity_comparisons(id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_step_comparison_step FOREIGN KEY (workout_step_id) REFERENCES workout_steps(id)
);
