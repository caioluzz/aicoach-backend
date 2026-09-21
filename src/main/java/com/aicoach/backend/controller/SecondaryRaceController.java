package com.aicoach.backend.controller;

import com.aicoach.backend.api.SecondaryRaceApi;
import com.aicoach.backend.dto.SecondaryRaceRequest;
import com.aicoach.backend.dto.SecondaryRaceResponse;
import com.aicoach.backend.service.SecondaryRaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SecondaryRaceController implements SecondaryRaceApi {
    private final SecondaryRaceService service;

    public SecondaryRaceResponse create(Long athleteId, SecondaryRaceRequest request) {
        return service.create(athleteId, request);
    }

    public List<SecondaryRaceResponse> list(Long athleteId) {
        return service.list(athleteId);
    }
}
