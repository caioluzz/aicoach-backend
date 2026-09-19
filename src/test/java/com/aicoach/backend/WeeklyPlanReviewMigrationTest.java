package com.aicoach.backend;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeeklyPlanReviewMigrationTest {
    @Test
    void migratesExistingWeeklyPlansAndCreatesValidationAuditTable() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:weekly_plan_review_migration;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE weekly_plans (id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "created_at DATETIME(6) NOT NULL)");
        jdbc.execute("INSERT INTO weekly_plans (created_at) VALUES (CURRENT_TIMESTAMP)");

        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V11__add_weekly_plan_review.sql"))
                .execute(dataSource);

        assertEquals("VALIDATED", jdbc.queryForObject(
                "SELECT status FROM weekly_plans WHERE id = 1", String.class));
        assertEquals(1, jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'PUBLIC'
                AND table_name = 'WEEKLY_PLAN_VALIDATION_MESSAGES'
                """, Integer.class));
        assertEquals(1, jdbc.queryForObject("""
                SELECT COUNT(*) FROM weekly_plans WHERE validated_at IS NOT NULL
                AND validator_version = 'weekly-plan-validator-1.0'
                """, Integer.class));
    }
}
