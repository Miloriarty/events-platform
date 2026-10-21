package org.example.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.example.booking.model.OrderStatus;
import org.example.booking.model.OrdersModel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Заказ")
public record OrderResponse(
        Long id,
        Long userId,
        LocalDateTime orderDate,
        BigDecimal totalPrice,
        OrderStatus status,
        List<OrderItemResponse> items
) {
    public static OrderResponse from(OrdersModel o) {
        return new OrderResponse(o.getId(), o.getUserId(), o.getOrderDate(), o.getTotalPrice(),
                o.getStatus(), o.getItems().stream().map(OrderItemResponse::from).toList());
    }
}