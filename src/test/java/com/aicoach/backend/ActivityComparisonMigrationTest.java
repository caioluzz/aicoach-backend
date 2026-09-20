package com.aicoach.backend;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActivityComparisonMigrationTest {
    @Test
    void createsReusableTotalAndStepResults() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:activity_comparison_migration;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE athletes (id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE activities (id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE planned_activities (id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE workout_steps (id BIGINT PRIMARY KEY)");

        new ResourceDatabasePopulator(
                new ClassPathResource("db/migration/V14__add_activity_comparisons.sql"))
                .execute(dataSource);

        assertEquals(1, countTable(jdbc, "ACTIVITY_COMPARISONS"));
        assertEquals(1, countTable(jdbc, "ACTIVITY_STEP_COMPARISONS"));
        assertEquals(2, jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.table_constraints
                WHERE table_schema = 'PUBLIC' AND table_name = 'ACTIVITY_COMPARISONS'
                AND constraint_type = 'UNIQUE'
                """, Integer.class));
    }

    private int countTable(JdbcTemplate jdbc, String name) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_schema = 'PUBLIC' AND table_name = '" + name + "'", Integer.class);
    }
}
