package org.example.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Запрос на регистрацию на мероприятие")
public record RegistrationRequest(

        @NotNull(message = "Event is required")
        @Schema(description = "ID мероприятия", example = "1")
        Long eventId,

        @NotNull(message = "Order item is required")
        @Schema(description = "ID позиции вашего заказа, по которой вы регистрируетесь", example = "1")
        Long orderItemId
) {}