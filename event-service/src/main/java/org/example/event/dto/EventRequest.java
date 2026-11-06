package org.example.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Данные для создания или изменения мероприятия")
public record EventRequest(

        @NotBlank(message = "Title is required")
        @Schema(description = "Название", example = "Spring Conference 2026")
        String title,

        @Schema(description = "Описание", example = "Большая конференция по Spring")
        String description,

        @NotNull(message = "Start date is required")
        @Schema(description = "Дата начала", example = "2026-12-15")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        @Schema(description = "Дата окончания", example = "2026-12-16")
        LocalDate endDate,

        @NotNull(message = "Location is required")
        @Schema(description = "ID локации", example = "1")
        Long locationId,

        @NotNull(message = "Category is required")
        @Schema(description = "ID категории", example = "1")
        Long categoryId,

        @Positive(message = "Max participants must be greater than 0")
        @Schema(description = "Максимум участников; если не указан, берётся вместимость локации",
                example = "100")
        Integer maxParticipants,

        @NotNull(message = "Minimum ticket price is required")
        @PositiveOrZero(message = "Minimum ticket price must not be negative")
        @Digits(integer = 10, fraction = 2, message = "Minimum ticket price is not a valid amount")
        @Schema(description = "Минимальная цена билета для регистрации; 0 — подходит любой билет",
                example = "1000.00")
        BigDecimal minTicketPrice
) {}