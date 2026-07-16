package com.aicoach.backend.domain;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "athletes")
@Data
public class Athlete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
}

