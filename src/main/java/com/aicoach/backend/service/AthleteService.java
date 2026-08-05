package com.aicoach.backend.service;

import com.aicoach.backend.dto.AthleteRequestDTO;
import com.aicoach.backend.enums.ObjectiveStatus;
import com.aicoach.backend.enums.RacePriority;
import com.aicoach.backend.models.Athlete;
import com.aicoach.backend.models.Objective;
import com.aicoach.backend.repository.AthleteRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AthleteService {

    private final AthleteRepo athleteRepo;

    public Athlete saveAthlete(AthleteRequestDTO request) {
        Athlete athlete = new Athlete();
        athlete.setName(request.name());
        athlete.setGarminEmail(request.garminEmail());
        athlete.setGarminPassword(request.garminPassword());
        athlete.setDateOfBirth(request.dateOfBirth());
        athlete.setWeightKg(request.weightKg());
        athlete.setHeightCm(request.heightCm());
        athlete.setGender(request.gender());
        athlete.setAvailableTrainingDays(request.availableTrainingDays());

        if (request.objectives() != null && !request.objectives().isEmpty()) {
            List<Objective> objectives = request.objectives().stream().map(objDto -> {
                Objective obj = new Objective();
                obj.setTitle(objDto.title());
                obj.setTargetDistance_m(objDto.distanceMeters());
                obj.setTargetDate(objDto.targetDate());

                obj.setPriority(RacePriority.valueOf(objDto.priority()));
                obj.setStatus(ObjectiveStatus.valueOf(objDto.status()));

                obj.setAthlete(athlete);

                return obj;
            }).collect(Collectors.toList());

            athlete.setObjectives(objectives);
        }

        return athleteRepo.save(athlete);
    }

    public List<Athlete> getAllAthletes() {
        return athleteRepo.findAll();
    }
}
