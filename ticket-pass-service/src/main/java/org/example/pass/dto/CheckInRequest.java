package org.example.pass.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Проверка билета на входе")
public record CheckInRequest(
        @NotBlank @Schema(description = "Токен из QR-кода") String token
) {}
