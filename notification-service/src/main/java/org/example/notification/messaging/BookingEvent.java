package org.example.notification.messaging;

import org.example.notification.model.NotificationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Контракт сообщения из booking-service. Поля заказа и регистрации заполнены в зависимости от type
public record BookingEvent(
        String messageId,
        NotificationType type,
        Long userId,
        Long orderId,
        BigDecimal totalPrice,
        Long registrationId,
        Long eventId,
        String eventTitle,
        LocalDateTime occurredAt
) {}
