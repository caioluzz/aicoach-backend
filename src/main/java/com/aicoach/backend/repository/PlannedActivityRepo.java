package com.aicoach.backend.repository;

import com.aicoach.backend.models.PlannedActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import com.aicoach.backend.enums.WeeklyPlanStatus;
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

    @Query("""
            select p from PlannedActivity p
            left join ActivityComparison c on c.plannedActivity = p
            where p.weeklyPlan.athlete.id = :athleteId
              and p.weeklyPlan.status in :statuses
              and p.scheduledDate between :from and :to
              and c.id is null
            order by p.scheduledDate, p.sessionOrder, p.id
            """)
    List<PlannedActivity> findUnexecutedUpcoming(
            @Param("athleteId") Long athleteId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("statuses") Collection<WeeklyPlanStatus> statuses);

    @Query("""
            select p from PlannedActivity p
            join fetch p.weeklyPlan wp
            where wp.athlete.id = :athleteId
              and wp.globalPlan.id = :globalPlanId
              and wp.status in :statuses
              and p.scheduledDate between :from and :to
            order by p.scheduledDate, p.sessionOrder, p.id
            """)
    List<PlannedActivity> findSecondaryRaceWindow(
            @Param("athleteId") Long athleteId,
            @Param("globalPlanId") Long globalPlanId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("statuses") Collection<WeeklyPlanStatus> statuses);
}
