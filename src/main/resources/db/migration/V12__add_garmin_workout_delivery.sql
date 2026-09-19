CREATE TABLE garmin_workout_deliveries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    athlete_id BIGINT NOT NULL,
    weekly_plan_id BIGINT NOT NULL,
    planned_activity_id BIGINT NOT NULL,
    plan_version INT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    scheduled_date DATE NOT NULL,
    external_workout_id BIGINT,
    external_schedule_id BIGINT,
    attempt_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    scheduled_at DATETIME(6),
    confirmed_at DATETIME(6),
    cancelled_at DATETIME(6),
    lock_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_garmin_delivery_idempotency
        UNIQUE (athlete_id, planned_activity_id, plan_version, content_hash),
    CONSTRAINT uk_garmin_delivery_key UNIQUE (idempotency_key),
    CONSTRAINT fk_garmin_delivery_athlete FOREIGN KEY (athlete_id) REFERENCES athletes(id),
    CONSTRAINT fk_garmin_delivery_weekly_plan FOREIGN KEY (weekly_plan_id)
        REFERENCES weekly_plans(id) ON DELETE CASCADE,
    CONSTRAINT fk_garmin_delivery_activity FOREIGN KEY (planned_activity_id)
        REFERENCES planned_activities(id) ON DELETE CASCADE
);

CREATE INDEX idx_garmin_delivery_plan_status
    ON garmin_workout_deliveries (weekly_plan_id, status);
