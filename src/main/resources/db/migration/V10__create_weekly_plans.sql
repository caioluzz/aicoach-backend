CREATE TABLE weekly_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    athlete_id BIGINT NOT NULL,
    global_plan_id BIGINT NOT NULL,
    season_plan_week_id BIGINT NOT NULL,
    training_cycle_id BIGINT NOT NULL,
    assessment_id BIGINT NOT NULL,
    athlete_metrics_id BIGINT NOT NULL,
    version INT NOT NULL,
    week_start DATE NOT NULL,
    week_end DATE NOT NULL,
    target_volume_km DOUBLE NOT NULL,
    planned_distance_m INT NOT NULL,
    planned_duration_s INT NOT NULL,
    summary VARCHAR(2000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    generation_source VARCHAR(20) NOT NULL,
    openai_response_id VARCHAR(100),
    model VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(40) NOT NULL,
    schema_version VARCHAR(20) NOT NULL,
    input_tokens INT,
    output_tokens INT,
    generation_latency_ms BIGINT,
    CONSTRAINT uk_weekly_plan_week_version UNIQUE (season_plan_week_id, version),
    CONSTRAINT fk_weekly_plan_athlete FOREIGN KEY (athlete_id) REFERENCES athletes(id),
    CONSTRAINT fk_weekly_plan_global FOREIGN KEY (global_plan_id) REFERENCES global_plans(id) ON DELETE CASCADE,
    CONSTRAINT fk_weekly_plan_week FOREIGN KEY (season_plan_week_id) REFERENCES season_plan_weeks(id),
    CONSTRAINT fk_weekly_plan_cycle FOREIGN KEY (training_cycle_id) REFERENCES training_cycles(id),
    CONSTRAINT fk_weekly_plan_assessment FOREIGN KEY (assessment_id) REFERENCES athlete_assessments(id),
    CONSTRAINT fk_weekly_plan_metrics FOREIGN KEY (athlete_metrics_id) REFERENCES athlete_metrics(id)
);

ALTER TABLE planned_activities ADD COLUMN weekly_plan_id BIGINT;
ALTER TABLE planned_activities ADD COLUMN session_order INT;
ALTER TABLE planned_activities ADD COLUMN name VARCHAR(200);
ALTER TABLE planned_activities ADD COLUMN planned_distance_m INT;
ALTER TABLE planned_activities ADD COLUMN planned_duration_s INT;
ALTER TABLE planned_activities ADD CONSTRAINT fk_activity_weekly_plan
    FOREIGN KEY (weekly_plan_id) REFERENCES weekly_plans(id) ON DELETE CASCADE;
ALTER TABLE planned_activities ADD CONSTRAINT uk_weekly_plan_session_order
    UNIQUE (weekly_plan_id, session_order);

ALTER TABLE workout_blocks ADD CONSTRAINT uk_activity_block_order
    UNIQUE (planned_activity_id, block_order);

ALTER TABLE workout_steps ADD COLUMN target_pace_fastest_sec_per_km INT;
ALTER TABLE workout_steps ADD COLUMN target_pace_slowest_sec_per_km INT;
ALTER TABLE workout_steps ADD COLUMN instruction VARCHAR(500);
ALTER TABLE workout_steps ADD CONSTRAINT uk_block_step_order
    UNIQUE (workout_block_id, step_order);
