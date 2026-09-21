package com.aicoach.backend.api;

import com.aicoach.backend.dto.ConfigurationStatusResponse;
import com.aicoach.backend.dto.GarminCredentialsRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/configuration")
public interface ConfigurationApi {
    @GetMapping("/athletes/{athleteId}")
    ConfigurationStatusResponse get(@PathVariable Long athleteId);

    @PutMapping("/athletes/{athleteId}/garmin")
    ConfigurationStatusResponse updateGarmin(@PathVariable Long athleteId,
                                             @Valid @RequestBody GarminCredentialsRequest request);
}
