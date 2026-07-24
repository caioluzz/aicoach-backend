package com.aicoach.backend.dto;

public record GarminBotRequestDTO(
        String email,
        String password,
        int limit)
{}
