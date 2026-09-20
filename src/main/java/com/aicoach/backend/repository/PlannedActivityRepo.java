package com.aicoach.backend.repository;

import com.aicoach.backend.models.PlannedActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface PlannedActivityRepo extends JpaRepository<PlannedActivity, Long> {
    @Query("""
            select p from PlannedActivity p
            join p.trainingCycle tc
            join tc.globalPlan gp
            where gp.athlete.id = :athleteId and p.scheduledDate between :from and :to
            order by p.scheduledDate, p.id
            """)
    List<PlannedActivity> findComparisonCandidates(@Param("athleteId") Long athleteId,
                                                    @Param("from") LocalDate from,
                                                    @Param("to") LocalDate to);
}
