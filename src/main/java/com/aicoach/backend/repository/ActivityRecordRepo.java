package com.aicoach.backend.repository;

import com.aicoach.backend.models.ActivityRecord;
import com.aicoach.backend.models.ActivityRecordId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRecordRepo extends JpaRepository<ActivityRecord, ActivityRecordId> {
    @Query("select record from ActivityRecord record where record.activity.id = :activityId order by record.id.ts")
    List<ActivityRecord> findForActivityOrdered(@Param("activityId") Long activityId);
}
