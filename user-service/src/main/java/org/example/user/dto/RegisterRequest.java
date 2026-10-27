package org.example.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Schema(description = "Логин", example = "user1")
        String username,
        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        @Schema(description = "Пароль, минимум 6 символов", example = "password123")
        String password,
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Schema(description = "Email", example = "user1@example.com")
        String email,
        @NotBlank(message = "Full name is required")
        @Schema(description = "Полное имя", example = "Ivan Ivanov")
        String fullName,
        @NotBlank(message = "Phone is required")
        @Schema(description = "Телефон", example = "+79990000000")
        String phone
) {}
