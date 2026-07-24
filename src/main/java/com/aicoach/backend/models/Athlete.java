package com.aicoach.backend.models;

import com.aicoach.backend.utils.EncryptionConverter;
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

    @Column(name = "garmin_email")
    private String garminEmail;

    @Column(name = "garmin_password")
    @Convert(converter = EncryptionConverter.class)
    private String garminPassword;
}

