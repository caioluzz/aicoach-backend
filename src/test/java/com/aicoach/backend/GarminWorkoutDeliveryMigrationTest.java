package com.aicoach.backend;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GarminWorkoutDeliveryMigrationTest {
    @Test
    void createsDeliveryAuditAndIdempotencyConstraints() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:garmin_delivery_migration;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE athletes (id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE weekly_plans (id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE planned_activities (id BIGINT PRIMARY KEY)");

        new ResourceDatabasePopulator(
                new ClassPathResource("db/migration/V12__add_garmin_workout_delivery.sql"))
                .execute(dataSource);

        assertEquals(1, jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'PUBLIC'
                AND table_name = 'GARMIN_WORKOUT_DELIVERIES'
                """, Integer.class));
        assertEquals(2, jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.table_constraints
                WHERE table_schema = 'PUBLIC' AND table_name = 'GARMIN_WORKOUT_DELIVERIES'
                AND constraint_type = 'UNIQUE'
                """, Integer.class));
    }
}
