package com.aicoach.backend.service;

import com.aicoach.backend.models.Activity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.aicoach.backend.repository.ActivityRepo;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepo activityRepo;

    @Transactional
    public Activity saveActivity(Activity activity) {
        if (activity.getGarminActivityId() != null &&
                activityRepo.existsByGarminActivityId(activity.getGarminActivityId())) {
            throw new IllegalArgumentException("Atividade do Garmin já registrada no sistema.");
        }

        return activityRepo.save(activity);
    }
}
