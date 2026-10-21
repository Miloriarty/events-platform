package org.example.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Позиция заказа в запросе")
public record OrderItemRequest(

        @NotNull(message = "Ticket is required")
        @Schema(description = "ID типа билета", example = "1")
        Long ticketId,

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than 0")
        @Schema(description = "Количество билетов", example = "2")
        Integer quantity
) {

}