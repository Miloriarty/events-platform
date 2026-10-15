package org.example.user.dto;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresInSeconds
) {}