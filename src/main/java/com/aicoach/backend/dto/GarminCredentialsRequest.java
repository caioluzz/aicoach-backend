package com.aicoach.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record GarminCredentialsRequest(
        @NotBlank @Email String email,
        @NotBlank String password) {
}
