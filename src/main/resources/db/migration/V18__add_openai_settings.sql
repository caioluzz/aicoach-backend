CREATE TABLE athlete_openai_settings (
    athlete_id BIGINT PRIMARY KEY,
    api_key TEXT NOT NULL,
    planner_model VARCHAR(100) NOT NULL,
    weekly_planner_model VARCHAR(100) NOT NULL,
    activity_review_model VARCHAR(100) NOT NULL,
    max_output_tokens INT NOT NULL,
    weekly_max_output_tokens INT NOT NULL,
    activity_review_max_output_tokens INT NOT NULL,
    last_validated_at TIMESTAMP NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_openai_settings_athlete FOREIGN KEY (athlete_id) REFERENCES athletes(id) ON DELETE CASCADE
);
