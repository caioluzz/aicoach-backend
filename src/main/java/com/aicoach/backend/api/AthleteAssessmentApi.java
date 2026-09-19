package com.aicoach.backend.api;

import com.aicoach.backend.dto.AthleteAssessmentRequest;
import com.aicoach.backend.dto.AthleteAssessmentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/athletes/{athleteId}/assessments")
public interface AthleteAssessmentApi {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AthleteAssessmentResponse createVersion(@PathVariable Long athleteId,
                                              @Valid @RequestBody AthleteAssessmentRequest request);

    @GetMapping("/latest")
    AthleteAssessmentResponse getLatest(@PathVariable Long athleteId);

    @GetMapping
    List<AthleteAssessmentResponse> getHistory(@PathVariable Long athleteId);
}
