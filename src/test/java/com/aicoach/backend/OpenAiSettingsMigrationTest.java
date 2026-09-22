package com.aicoach.backend;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenAiSettingsMigrationTest {
    @Test
    void createsEncryptedPerAthleteOpenAiSettingsTable() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:openai_settings_migration;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE athletes (id BIGINT PRIMARY KEY)");

        new ResourceDatabasePopulator(new ClassPathResource(
                "db/migration/V18__add_openai_settings.sql")).execute(dataSource);

        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_schema = 'PUBLIC' AND table_name = 'ATHLETE_OPENAI_SETTINGS'", Integer.class));
    }
}
