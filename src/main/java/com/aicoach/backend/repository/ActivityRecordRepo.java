package com.aicoach.backend.repository;

import com.aicoach.backend.models.ActivityRecord;
import com.aicoach.backend.models.ActivityRecordId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityRecordRepo extends JpaRepository<ActivityRecord, ActivityRecordId> {
}
