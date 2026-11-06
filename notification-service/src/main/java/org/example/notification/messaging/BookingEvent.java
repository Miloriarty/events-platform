package org.example.notification.messaging;

import org.example.notification.model.NotificationType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Контракт сообщений из booking-service и reminder-service. Поля заполнены в зависимости от type,
// eventDate приходит только в напоминании (EVENT_REMINDER)
public record BookingEvent(
        String messageId,
        NotificationType type,
        Long userId,
        Long orderId,
        BigDecimal totalPrice,
        Long registrationId,
        Long eventId,
        String eventTitle,
        LocalDateTime occurredAt,
        LocalDate eventDate
) {}
