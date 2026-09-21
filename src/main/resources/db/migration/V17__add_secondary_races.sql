CREATE TABLE secondary_races (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    athlete_id BIGINT NOT NULL,
    global_plan_id BIGINT NOT NULL,
    primary_objective_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    race_date DATE NOT NULL,
    distance_m INT NOT NULL,
    target_time_s INT,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    rule_version VARCHAR(50) NOT NULL,
    window_start DATE NOT NULL,
    window_end DATE NOT NULL,
    rationale VARCHAR(2000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_secondary_race_plan_date UNIQUE (global_plan_id, race_date),
    CONSTRAINT ck_secondary_race_distance CHECK (distance_m BETWEEN 1000 AND 100000),
    CONSTRAINT ck_secondary_race_priority CHECK (priority IN ('B_RACE', 'C_RACE')),
    CONSTRAINT fk_secondary_race_athlete FOREIGN KEY (athlete_id) REFERENCES athletes(id),
    CONSTRAINT fk_secondary_race_plan FOREIGN KEY (global_plan_id) REFERENCES global_plans(id),
    CONSTRAINT fk_secondary_race_primary FOREIGN KEY (primary_objective_id) REFERENCES objectives(id)
);

CREATE INDEX idx_secondary_race_athlete_date ON secondary_races (athlete_id, race_date);

CREATE TABLE secondary_race_adjustments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    secondary_race_id BIGINT NOT NULL,
    weekly_plan_id BIGINT,
    planned_activity_id BIGINT,
    adjustment_window VARCHAR(20) NOT NULL,
    action VARCHAR(30) NOT NULL,
    original_date DATE,
    proposed_date DATE NOT NULL,
    original_workout_type VARCHAR(30),
    proposed_workout_type VARCHAR(30) NOT NULL,
    original_distance_m INT,
    proposed_distance_m INT,
    original_duration_s INT,
    proposed_duration_s INT,
    load_percent INT NOT NULL,
    rationale VARCHAR(500) NOT NULL,
    CONSTRAINT ck_secondary_adjustment_load CHECK (load_percent BETWEEN 0 AND 100),
    CONSTRAINT fk_secondary_adjustment_race FOREIGN KEY (secondary_race_id)
        REFERENCES secondary_races(id) ON DELETE CASCADE,
    CONSTRAINT fk_secondary_adjustment_weekly FOREIGN KEY (weekly_plan_id)
        REFERENCES weekly_plans(id),
    CONSTRAINT fk_secondary_adjustment_planned FOREIGN KEY (planned_activity_id)
        REFERENCES planned_activities(id)
);

CREATE INDEX idx_secondary_adjustment_race ON secondary_race_adjustments (secondary_race_id, proposed_date);
