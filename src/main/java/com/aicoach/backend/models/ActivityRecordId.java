package com.aicoach.backend.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRecordId implements Serializable {

    @Column(name = "activity_id")
    private Long activityId;

    @Column(name = "ts")
    private LocalDateTime ts;
}