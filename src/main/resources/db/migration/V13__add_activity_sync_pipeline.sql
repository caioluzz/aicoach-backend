CREATE TABLE activity_sync_state (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    athlete_id BIGINT NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'NEVER_RUN',
    checkpoint_started_at DATETIME,
    last_attempt_at DATETIME,
    last_success_at DATETIME,
    last_error VARCHAR(1000),
    discovered_count INT NOT NULL DEFAULT 0,
    imported_count INT NOT NULL DEFAULT 0,
    skipped_count INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_activity_sync_state_athlete
        FOREIGN KEY (athlete_id) REFERENCES athletes(id) ON DELETE CASCADE
);

CREATE INDEX idx_activities_athlete_started_at
    ON activities (athlete_id, started_at);
