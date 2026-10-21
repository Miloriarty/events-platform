package org.example.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.example.booking.model.OrderItemsModel;

import java.math.BigDecimal;

@Schema(description = "Позиция заказа")
public record OrderItemResponse(
        Long id,
        Long ticketId,
        String ticketType,
        Integer quantity,
        @Schema(description = "Цена за один билет на момент покупки") BigDecimal unitPrice
) {
    public static OrderItemResponse from(OrderItemsModel i) {
        return new OrderItemResponse(i.getId(), i.getTicket().getId(), i.getTicketType(),
                i.getQuantity(), i.getUnitPrice());
    }
}