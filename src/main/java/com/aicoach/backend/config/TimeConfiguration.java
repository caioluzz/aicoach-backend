package com.aicoach.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import com.aicoach.backend.training.daniels.DanielsPerformanceEngine;

@Configuration
public class TimeConfiguration {
    @Bean
    Clock systemClock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    DanielsPerformanceEngine danielsPerformanceEngine(Clock clock) {
        return new DanielsPerformanceEngine(clock);
    }
}
