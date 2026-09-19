package com.aicoach.backend;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AthleteAssessmentMigrationTest {

    @Test
    void createsAssessmentSchemaFromV8Migration() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:assessment_migration;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE athletes (id BIGINT PRIMARY KEY)");

        new ResourceDatabasePopulator(
                new ClassPathResource("db/migration/V8__create_athlete_assessments.sql"))
                .execute(dataSource);

        Integer tableCount = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = 'PUBLIC'
                  AND table_name IN ('ATHLETE_ASSESSMENTS', 'ASSESSMENT_AVAILABILITY',
                                     'ASSESSMENT_SURFACES', 'ASSESSMENT_EQUIPMENT',
                                     'ASSESSMENT_HEALTH_ISSUES')
                """, Integer.class);
        assertEquals(5, tableCount);
    }
}
