-- 1. Atualização da tabela de Atletas já existente
ALTER TABLE athletes
ADD COLUMN date_of_birth DATE,
ADD COLUMN weight_kg DOUBLE,
ADD COLUMN height_cm INT,
ADD COLUMN gender VARCHAR(50),
ADD COLUMN max_heart_rate INT,
ADD COLUMN resting_heart_rate INT;

-- 2. Tabela auxiliar de Dias de Treinamento (ElementCollection)
CREATE TABLE athlete_training_days (
   athlete_id BIGINT NOT NULL,
   day_of_week VARCHAR(20) NOT NULL,
   CONSTRAINT fk_athlete_training_days FOREIGN KEY (athlete_id) REFERENCES athletes(id) ON DELETE CASCADE
);

-- 3. Objetivos (Depende de Atleta)[cite: 5]
CREATE TABLE objectives (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    athlete_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    target_date DATE NOT NULL,
    target_distance_m INT NOT NULL,
    target_time_minutes INT,
    priority VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    CONSTRAINT fk_objective_athlete FOREIGN KEY (athlete_id) REFERENCES athletes(id) ON DELETE CASCADE
);

-- 4. Métricas do Atleta (Depende de Atleta)[cite: 7]
CREATE TABLE athlete_metrics (
     id BIGINT AUTO_INCREMENT PRIMARY KEY,
     athlete_id BIGINT NOT NULL,
     recorded_at DATETIME NOT NULL,
     vdot DOUBLE NOT NULL,
     easy_pace_sec INT,
     marathon_pace_sec INT,
     threshold_pace_sec INT,
     interval_pace_sec INT,
     repetition_pace_sec INT,
     CONSTRAINT fk_metrics_athlete FOREIGN KEY (athlete_id) REFERENCES athletes(id) ON DELETE CASCADE
);

-- 5. Plano Global (Depende de Atleta e Objetivo)[cite: 6]
CREATE TABLE global_plans (
      id BIGINT AUTO_INCREMENT PRIMARY KEY,
      athlete_id BIGINT NOT NULL,
      objective_id BIGINT NOT NULL,
      start_date DATE NOT NULL,
      end_date DATE NOT NULL,
      total_weeks INT NOT NULL,
      CONSTRAINT fk_plan_athlete FOREIGN KEY (athlete_id) REFERENCES athletes(id) ON DELETE CASCADE,
      CONSTRAINT fk_plan_objective FOREIGN KEY (objective_id) REFERENCES objectives(id) ON DELETE CASCADE
);

-- 6. Ciclos de Treino / Mesociclos (Depende de Plano Global)[cite: 3]
CREATE TABLE training_cycles (
     id BIGINT AUTO_INCREMENT PRIMARY KEY,
     global_plan_id BIGINT NOT NULL,
     phase VARCHAR(50) NOT NULL,
     max_weekly_volume_km DOUBLE NOT NULL,
     max_stress_points INT,
     CONSTRAINT fk_cycle_plan FOREIGN KEY (global_plan_id) REFERENCES global_plans(id) ON DELETE CASCADE
);

-- 7. Atividades Planejadas / Microciclos (Depende de Ciclo)[cite: 4]
CREATE TABLE planned_activities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    training_cycle_id BIGINT NOT NULL,
    scheduled_date DATE NOT NULL,
    workout_type VARCHAR(50) NOT NULL,
    calculated_stress_points INT,
    CONSTRAINT fk_activity_cycle FOREIGN KEY (training_cycle_id) REFERENCES training_cycles(id) ON DELETE CASCADE
);

-- 8. Blocos de Treino (Depende de Atividade Planejada)[cite: 1]
CREATE TABLE workout_blocks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    planned_activity_id BIGINT NOT NULL,
    block_order INT NOT NULL,
    iterations INT NOT NULL DEFAULT 1,
    CONSTRAINT fk_block_activity FOREIGN KEY (planned_activity_id) REFERENCES planned_activities(id) ON DELETE CASCADE
);

-- 9. Passos do Treino (Depende de Bloco e de Atividade Planejada)[cite: 2]
CREATE TABLE workout_steps (
   id BIGINT AUTO_INCREMENT PRIMARY KEY,
   planned_activity_id BIGINT NOT NULL,
   workout_block_id BIGINT NOT NULL,
   step_order INT NOT NULL,
   step_type VARCHAR(50) NOT NULL,
   duration_type VARCHAR(50) NOT NULL,
   duration_value INT NOT NULL,
   target_zone VARCHAR(50) NOT NULL,
   CONSTRAINT fk_step_activity FOREIGN KEY (planned_activity_id) REFERENCES planned_activities(id) ON DELETE CASCADE,
   CONSTRAINT fk_step_block FOREIGN KEY (workout_block_id) REFERENCES workout_blocks(id) ON DELETE CASCADE
);