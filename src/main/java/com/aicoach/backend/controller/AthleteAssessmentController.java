package com.aicoach.backend.controller;

import com.aicoach.backend.api.AthleteAssessmentApi;
import com.aicoach.backend.dto.AthleteAssessmentRequest;
import com.aicoach.backend.dto.AthleteAssessmentResponse;
import com.aicoach.backend.service.AthleteAssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AthleteAssessmentController implements AthleteAssessmentApi {

    private final AthleteAssessmentService assessmentService;

    @Override
    public AthleteAssessmentResponse createVersion(Long athleteId, AthleteAssessmentRequest request) {
        return assessmentService.createVersion(athleteId, request);
    }

    @Override
    public AthleteAssessmentResponse getLatest(Long athleteId) {
        return assessmentService.getLatest(athleteId);
    }

    @Override
    public List<AthleteAssessmentResponse> getHistory(Long athleteId) {
        return assessmentService.getHistory(athleteId);
    }
}
