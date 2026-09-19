package com.aicoach.backend;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeasonPlanMigrationTest {
    @Test
    void upgradesSeasonPlanSchema() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:season_plan_migration;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE athletes (id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE objectives (id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE athlete_assessments (id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE athlete_metrics (id BIGINT PRIMARY KEY)");
        jdbc.execute("""
                CREATE TABLE global_plans (id BIGINT AUTO_INCREMENT PRIMARY KEY, athlete_id BIGINT NOT NULL,
                objective_id BIGINT NOT NULL, start_date DATE NOT NULL, end_date DATE NOT NULL, total_weeks INT NOT NULL)
                """);
        jdbc.execute("""
                CREATE TABLE training_cycles (id BIGINT AUTO_INCREMENT PRIMARY KEY, global_plan_id BIGINT NOT NULL,
                phase VARCHAR(50) NOT NULL, max_weekly_volume_km DOUBLE NOT NULL, max_stress_points INT)
                """);

        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V9__version_season_plans.sql"))
                .execute(dataSource);

        Integer tableCount = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'PUBLIC'
                AND table_name IN ('SEASON_PLAN_WEEKS', 'PLAN_REVISION_CRITERIA')
                """, Integer.class);
        Integer columnCount = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'PUBLIC'
                AND table_name = 'GLOBAL_PLANS' AND column_name IN ('VERSION', 'STATUS', 'ASSESSMENT_ID',
                'ATHLETE_METRICS_ID', 'PROMPT_VERSION', 'SCHEMA_VERSION')
                """, Integer.class);
        assertEquals(2, tableCount);
        assertEquals(6, columnCount);
    }
}
