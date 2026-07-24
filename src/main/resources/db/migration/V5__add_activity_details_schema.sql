ALTER TABLE activities
ADD COLUMN sport VARCHAR(30),
ADD COLUMN sub_sport VARCHAR(30),
ADD COLUMN ended_at DATETIME,
ADD COLUMN max_speed_kmh DECIMAL(5,2),
ADD COLUMN avg_pace_s_per_km SMALLINT,
ADD COLUMN best_pace_s_per_km SMALLINT,
ADD COLUMN max_hr SMALLINT,
ADD COLUMN avg_cadence SMALLINT,
ADD COLUMN max_cadence SMALLINT,
ADD COLUMN elevation_gain_m SMALLINT,
ADD COLUMN elevation_loss_m SMALLINT,
ADD COLUMN min_altitude_m DECIMAL(5,1),
ADD COLUMN max_altitude_m DECIMAL(5,1),
ADD COLUMN lap_count SMALLINT,
ADD COLUMN record_count SMALLINT,
ADD COLUMN raw_file_path VARCHAR(255),
ADD COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE laps (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    lap_number SMALLINT NOT NULL,
    start_time DATETIME,
    duration_s DECIMAL(7,3),
    distance_km DECIMAL(6,3),
    avg_pace_s_per_km SMALLINT,
    avg_speed_kmh DECIMAL(5,2),
    avg_hr SMALLINT,
    max_hr SMALLINT,
    avg_cadence SMALLINT,
    max_cadence SMALLINT,
    ascent_m SMALLINT,
    descent_m SMALLINT,
    CONSTRAINT fk_laps_activity FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE CASCADE
);

CREATE TABLE activity_records (
    activity_id BIGINT NOT NULL,
    ts DATETIME NOT NULL,
    elapsed_s SMALLINT,
    distance_km DECIMAL(6,3),
    speed_kmh DECIMAL(5,2),
    pace_s_per_km SMALLINT,
    heart_rate SMALLINT,
    cadence SMALLINT,
    altitude_m DECIMAL(5,1),
    PRIMARY KEY (activity_id, ts),
    CONSTRAINT fk_records_activity FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE CASCADE
);