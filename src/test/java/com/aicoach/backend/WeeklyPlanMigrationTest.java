package com.aicoach.backend;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeeklyPlanMigrationTest {
    @Test
    void createsWeeklyPlanSchemaWithoutReplacingExistingWorkoutTables() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:weekly_plan_migration;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE athletes (id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE athlete_assessments (id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE athlete_metrics (id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE global_plans (id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE season_plan_weeks (id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE training_cycles (id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE planned_activities (id BIGINT PRIMARY KEY, training_cycle_id BIGINT NOT NULL, "
                + "scheduled_date DATE NOT NULL, workout_type VARCHAR(50) NOT NULL, calculated_stress_points INT)");
        jdbc.execute("CREATE TABLE workout_blocks (id BIGINT PRIMARY KEY, planned_activity_id BIGINT NOT NULL, "
                + "block_order INT NOT NULL, iterations INT NOT NULL)");
        jdbc.execute("CREATE TABLE workout_steps (id BIGINT PRIMARY KEY, planned_activity_id BIGINT NOT NULL, "
                + "workout_block_id BIGINT NOT NULL, step_order INT NOT NULL, step_type VARCHAR(50) NOT NULL, "
                + "duration_type VARCHAR(50) NOT NULL, duration_value INT NOT NULL, target_zone VARCHAR(50) NOT NULL)");

        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V10__create_weekly_plans.sql"))
                .execute(dataSource);

        Integer tableCount = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'PUBLIC'
                AND table_name = 'WEEKLY_PLANS'
                """, Integer.class);
        Integer activityColumns = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'PUBLIC'
                AND table_name = 'PLANNED_ACTIVITIES' AND column_name IN
                ('WEEKLY_PLAN_ID', 'SESSION_ORDER', 'NAME', 'PLANNED_DISTANCE_M', 'PLANNED_DURATION_S')
                """, Integer.class);
        Integer stepColumns = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'PUBLIC'
                AND table_name = 'WORKOUT_STEPS' AND column_name IN
                ('TARGET_PACE_FASTEST_SEC_PER_KM', 'TARGET_PACE_SLOWEST_SEC_PER_KM', 'INSTRUCTION')
                """, Integer.class);
        assertEquals(1, tableCount);
        assertEquals(5, activityColumns);
        assertEquals(3, stepColumns);
    }
}
