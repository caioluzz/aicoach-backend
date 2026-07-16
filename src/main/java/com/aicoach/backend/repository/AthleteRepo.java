package com.aicoach.backend.repository;

import com.aicoach.backend.domain.Athlete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AthleteRepo extends JpaRepository<Athlete, Long> {
}