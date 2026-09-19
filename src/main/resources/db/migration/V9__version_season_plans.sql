ALTER TABLE global_plans ADD COLUMN assessment_id BIGINT;
ALTER TABLE global_plans ADD COLUMN athlete_metrics_id BIGINT;
ALTER TABLE global_plans ADD COLUMN version INT;
ALTER TABLE global_plans ADD COLUMN status VARCHAR(20);
ALTER TABLE global_plans ADD COLUMN summary VARCHAR(2000);
ALTER TABLE global_plans ADD COLUMN created_at DATETIME(6);
ALTER TABLE global_plans ADD COLUMN reviewed_at DATETIME(6);
ALTER TABLE global_plans ADD COLUMN review_comment VARCHAR(1000);
ALTER TABLE global_plans ADD COLUMN generation_source VARCHAR(20);
ALTER TABLE global_plans ADD COLUMN openai_response_id VARCHAR(100);
ALTER TABLE global_plans ADD COLUMN model VARCHAR(100);
ALTER TABLE global_plans ADD COLUMN prompt_version VARCHAR(40);
ALTER TABLE global_plans ADD COLUMN schema_version VARCHAR(20);
ALTER TABLE global_plans ADD COLUMN input_tokens INT;
ALTER TABLE global_plans ADD COLUMN output_tokens INT;
ALTER TABLE global_plans ADD COLUMN generation_latency_ms BIGINT;

ALTER TABLE global_plans ADD CONSTRAINT uk_global_plan_objective_version
    UNIQUE (athlete_id, objective_id, version);
ALTER TABLE global_plans ADD CONSTRAINT fk_plan_assessment
    FOREIGN KEY (assessment_id) REFERENCES athlete_assessments(id);
ALTER TABLE global_plans ADD CONSTRAINT fk_plan_metrics
    FOREIGN KEY (athlete_metrics_id) REFERENCES athlete_metrics(id);

ALTER TABLE training_cycles ADD COLUMN cycle_order INT;
ALTER TABLE training_cycles ADD COLUMN start_week INT;
ALTER TABLE training_cycles ADD COLUMN end_week INT;
ALTER TABLE training_cycles ADD COLUMN start_date DATE;
ALTER TABLE training_cycles ADD COLUMN end_date DATE;
ALTER TABLE training_cycles ADD COLUMN objective VARCHAR(500);
ALTER TABLE training_cycles ADD COLUMN expected_progression VARCHAR(1000);

CREATE TABLE season_plan_weeks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    global_plan_id BIGINT NOT NULL,
    week_number INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    target_volume_km DOUBLE NOT NULL,
    focus VARCHAR(500) NOT NULL,
    recovery_week BOOLEAN NOT NULL,
    taper_week BOOLEAN NOT NULL,
    CONSTRAINT uk_plan_week_number UNIQUE (global_plan_id, week_number),
    CONSTRAINT fk_week_plan FOREIGN KEY (global_plan_id) REFERENCES global_plans(id) ON DELETE CASCADE
);

CREATE TABLE plan_revision_criteria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    global_plan_id BIGINT NOT NULL,
    code VARCHAR(50) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    CONSTRAINT uk_plan_revision_code UNIQUE (global_plan_id, code),
    CONSTRAINT fk_revision_plan FOREIGN KEY (global_plan_id) REFERENCES global_plans(id) ON DELETE CASCADE
);
