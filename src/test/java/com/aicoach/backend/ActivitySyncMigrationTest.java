package com.aicoach.backend;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActivitySyncMigrationTest {
    @Test
    void createsCheckpointStateAndLookupIndex() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:activity_sync_migration;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE athletes (id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE activities (id BIGINT PRIMARY KEY, athlete_id BIGINT, started_at DATETIME)");

        new ResourceDatabasePopulator(
                new ClassPathResource("db/migration/V13__add_activity_sync_pipeline.sql"))
                .execute(dataSource);

        assertEquals(1, jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'PUBLIC'
                AND table_name = 'ACTIVITY_SYNC_STATE'
                """, Integer.class));
        assertEquals(1, jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.table_constraints
                WHERE table_schema = 'PUBLIC' AND table_name = 'ACTIVITY_SYNC_STATE'
                AND constraint_type = 'UNIQUE'
                """, Integer.class));
    }
}
