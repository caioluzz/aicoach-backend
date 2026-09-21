package com.aicoach.backend.api;

import com.aicoach.backend.dto.SecondaryRaceRequest;
import com.aicoach.backend.dto.SecondaryRaceResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/athletes/{athleteId}/secondary-races")
public interface SecondaryRaceApi {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SecondaryRaceResponse create(@PathVariable Long athleteId,
                                 @Valid @RequestBody SecondaryRaceRequest request);

    @GetMapping
    List<SecondaryRaceResponse> list(@PathVariable Long athleteId);
}
