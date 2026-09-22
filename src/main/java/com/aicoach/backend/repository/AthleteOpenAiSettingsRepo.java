package com.aicoach.backend.repository;

import com.aicoach.backend.models.AthleteOpenAiSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AthleteOpenAiSettingsRepo extends JpaRepository<AthleteOpenAiSettings, Long> {
}
