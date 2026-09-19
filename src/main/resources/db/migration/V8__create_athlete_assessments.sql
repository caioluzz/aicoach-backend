CREATE TABLE athlete_assessments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    athlete_id BIGINT NOT NULL,
    version INT NOT NULL,
    schema_version VARCHAR(20) NOT NULL,
    onboarding_status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6),
    date_of_birth DATE NOT NULL,
    weight_kg DOUBLE NOT NULL,
    height_cm INT NOT NULL,
    gender VARCHAR(20) NOT NULL,
    experience_level VARCHAR(20) NOT NULL,
    running_years INT NOT NULL,
    current_weekly_volume_km DOUBLE NOT NULL,
    recent_average_weekly_volume_km DOUBLE NOT NULL,
    recent_longest_run_km DOUBLE NOT NULL,
    current_runs_per_week INT NOT NULL,
    preferred_long_run_day VARCHAR(20) NOT NULL,
    strength_sessions_per_week INT NOT NULL,
    strength_training_notes VARCHAR(500),
    has_medical_restrictions BOOLEAN NOT NULL,
    medical_restrictions VARCHAR(1000),
    average_sleep_hours DOUBLE NOT NULL,
    sleep_quality VARCHAR(20) NOT NULL,
    recovery_days_per_week INT NOT NULL,
    routine_type VARCHAR(20) NOT NULL,
    routine_notes VARCHAR(500),
    target_race_title VARCHAR(255) NOT NULL,
    target_race_date DATE NOT NULL,
    target_race_distance_m INT NOT NULL,
    target_race_time_seconds INT NOT NULL,
    target_race_priority VARCHAR(20) NOT NULL,
    CONSTRAINT uk_assessment_athlete_version UNIQUE (athlete_id, version),
    CONSTRAINT fk_assessment_athlete FOREIGN KEY (athlete_id) REFERENCES athletes(id) ON DELETE CASCADE
);

CREATE TABLE assessment_availability (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assessment_id BIGINT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    available_minutes INT NOT NULL,
    CONSTRAINT uk_assessment_availability_day UNIQUE (assessment_id, day_of_week),
    CONSTRAINT fk_availability_assessment FOREIGN KEY (assessment_id)
        REFERENCES athlete_assessments(id) ON DELETE CASCADE
);

CREATE TABLE assessment_surfaces (
    assessment_id BIGINT NOT NULL,
    surface VARCHAR(20) NOT NULL,
    PRIMARY KEY (assessment_id, surface),
    CONSTRAINT fk_surface_assessment FOREIGN KEY (assessment_id)
        REFERENCES athlete_assessments(id) ON DELETE CASCADE
);

CREATE TABLE assessment_equipment (
    assessment_id BIGINT NOT NULL,
    equipment VARCHAR(40) NOT NULL,
    PRIMARY KEY (assessment_id, equipment),
    CONSTRAINT fk_equipment_assessment FOREIGN KEY (assessment_id)
        REFERENCES athlete_assessments(id) ON DELETE CASCADE
);

CREATE TABLE assessment_health_issues (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assessment_id BIGINT NOT NULL,
    body_area VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    pain_severity INT NOT NULL,
    started_on DATE,
    restriction_notes VARCHAR(500),
    CONSTRAINT fk_health_issue_assessment FOREIGN KEY (assessment_id)
        REFERENCES athlete_assessments(id) ON DELETE CASCADE
);
