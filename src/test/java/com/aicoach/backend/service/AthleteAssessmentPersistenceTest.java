package com.aicoach.backend.service;

import com.aicoach.backend.dto.AthleteAssessmentResponse;
import com.aicoach.backend.models.Athlete;
import com.aicoach.backend.repository.AthleteRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AthleteAssessmentPersistenceTest {

    @Autowired AthleteRepo athleteRepo;
    @Autowired AthleteAssessmentService service;

    @Test
    void persistsAndReadsVersionedAssessmentHistory() {
        Athlete athlete = new Athlete();
        athlete.setName("Runner");
        athlete = athleteRepo.saveAndFlush(athlete);

        AthleteAssessmentResponse first = service.createVersion(
                athlete.getId(), AthleteAssessmentServiceTest.validRequest(DayOfWeek.SUNDAY));
        AthleteAssessmentResponse second = service.createVersion(
                athlete.getId(), AthleteAssessmentServiceTest.validRequest(DayOfWeek.SUNDAY));

        assertEquals(1, first.version());
        assertEquals(2, second.version());
        assertNotNull(second.completedAt());
        assertEquals(second.id(), service.getLatest(athlete.getId()).id());
        assertEquals(List.of(2, 1), service.getHistory(athlete.getId()).stream()
                .map(AthleteAssessmentResponse::version).toList());
        assertEquals(2, service.getLatest(athlete.getId()).availability().size());
        assertEquals(1, service.getLatest(athlete.getId()).health().issues().size());
    }
}
