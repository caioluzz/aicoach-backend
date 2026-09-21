package com.aicoach.backend.api;

import com.aicoach.backend.dto.AdaptationDecisionResponse;
import com.aicoach.backend.dto.FeedbackRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/athletes/{athleteId}/feedback")
public interface FeedbackApi {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AdaptationDecisionResponse submit(@PathVariable Long athleteId,
                                      @Valid @RequestBody FeedbackRequest request);

    @GetMapping("/decisions")
    List<AdaptationDecisionResponse> history(@PathVariable Long athleteId);
}
