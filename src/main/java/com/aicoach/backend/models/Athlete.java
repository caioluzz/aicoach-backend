package com.aicoach.backend.models;

import com.aicoach.backend.enums.Gender;
import com.aicoach.backend.utils.EncryptionConverter;
import jakarta.persistence.*;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

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

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "weight_kg")
    private Double weightKg;

    @Column(name = "height_cm")
    private Integer heightCm;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    // Preenchidos posteriormente pelo motor de IA
    @Column(name = "max_heart_rate")
    private Integer maxHeartRate;

    @Column(name = "resting_heart_rate")
    private Integer restingHeartRate;

    // --- Atributos de Logística de Treino ---

    // Tabela auxiliar criada pelo Hibernate para guardar os dias da semana
    @ElementCollection(targetClass = DayOfWeek.class)
    @CollectionTable(name = "athlete_training_days", joinColumns = @JoinColumn(name = "athlete_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    private Set<DayOfWeek> availableTrainingDays;

    // --- Relacionamentos ---

    @OneToMany(mappedBy = "athlete", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Objective> objectives;

    @OneToMany(mappedBy = "athlete", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AthleteMetrics> metrics;

    @OneToMany(mappedBy = "athlete", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GlobalPlan> globalPlans;
}