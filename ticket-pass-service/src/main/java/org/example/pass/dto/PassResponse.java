package org.example.pass.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Электронный билет")
public record PassResponse(
        Long registrationId,
        Long eventId,
        String eventTitle,
        String ticketType,
        LocalDate startDate,
        LocalDate endDate,
        String locationName,
        String locationAddress,
        @Schema(description = "Подписанный токен билета") String token,
        @Schema(description = "Адрес страницы проверки, он же содержимое QR-кода") String checkInUrl,
        @Schema(description = "QR-код: data URI с PNG") String qrCode
) {}
