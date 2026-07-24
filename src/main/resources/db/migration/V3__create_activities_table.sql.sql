ALTER TABLE activities
ADD COLUMN garmin_activity_id BIGINT UNIQUE,
ADD COLUMN name VARCHAR(255),
ADD COLUMN distance_meters DOUBLE,
ADD COLUMN duration_seconds DOUBLE,
ADD COLUMN average_heart_rate INT,
ADD COLUMN average_speed DOUBLE;