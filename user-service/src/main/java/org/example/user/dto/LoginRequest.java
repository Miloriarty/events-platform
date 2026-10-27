package org.example.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Username is required")
        @Schema(description = "Логин", example = "admin")
        String username,
        @NotBlank(message = "Password is required")
        @Schema(description = "Пароль", example = "admin123")
        String password
) {}
