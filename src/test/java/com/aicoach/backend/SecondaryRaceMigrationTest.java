package com.aicoach.backend;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecondaryRaceMigrationTest {
    @Test
    void createsRaceAndAuditableAdjustmentTables() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:secondary_race_migration;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE athletes (id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE objectives (id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE global_plans (id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE weekly_plans (id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE planned_activities (id BIGINT PRIMARY KEY)");

        new ResourceDatabasePopulator(new ClassPathResource(
                "db/migration/V17__add_secondary_races.sql")).execute(dataSource);

        assertEquals(1, countTable(jdbc, "SECONDARY_RACES"));
        assertEquals(1, countTable(jdbc, "SECONDARY_RACE_ADJUSTMENTS"));
    }

    private int countTable(JdbcTemplate jdbc, String name) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_schema = 'PUBLIC' AND table_name = '" + name + "'", Integer.class);
    }
}
