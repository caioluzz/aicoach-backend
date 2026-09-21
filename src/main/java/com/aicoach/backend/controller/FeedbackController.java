package com.aicoach.backend.controller;

import com.aicoach.backend.api.FeedbackApi;
import com.aicoach.backend.dto.AdaptationDecisionResponse;
import com.aicoach.backend.dto.FeedbackRequest;
import com.aicoach.backend.service.FeedbackAdaptationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FeedbackController implements FeedbackApi {
    private final FeedbackAdaptationService service;

    public AdaptationDecisionResponse submit(Long athleteId, FeedbackRequest request) {
        return service.submit(athleteId, request);
    }

    public List<AdaptationDecisionResponse> history(Long athleteId) {
        return service.history(athleteId);
    }
}
