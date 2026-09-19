package com.aicoach.backend.repository;

import com.aicoach.backend.models.Athlete;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AthleteRepo extends JpaRepository<Athlete, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select athlete from Athlete athlete where athlete.id = :id")
    Optional<Athlete> findByIdForUpdate(@Param("id") Long id);
}
