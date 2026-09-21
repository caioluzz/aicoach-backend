package com.aicoach.backend.api;

import com.aicoach.backend.dto.VdotTestWorkoutResponse;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/athletes/{athleteId}/vdot-test/workout")
public interface VdotTestApi {
    @PostMapping("/preview")
    VdotTestWorkoutResponse preview(@PathVariable Long athleteId);

    @PostMapping("/deliver")
    VdotTestWorkoutResponse deliver(@PathVariable Long athleteId);
}
