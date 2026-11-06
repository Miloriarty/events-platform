package org.example.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.example.booking.model.OrderItemsModel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Позиция заказа, по которой можно зарегистрироваться")
public record AvailableOrderItem(
        Long orderItemId,
        Long orderId,
        LocalDateTime orderDate,
        String ticketType,
        BigDecimal unitPrice,
        Integer quantity,
        @Schema(description = "Сколько билетов позиции ещё не использовано") long remaining
) {
    public static AvailableOrderItem from(OrderItemsModel item, long remaining) {
        return new AvailableOrderItem(item.getId(), item.getOrder().getId(), item.getOrder().getOrderDate(),
                item.getTicketType(), item.getUnitPrice(), item.getQuantity(), remaining);
    }
}
