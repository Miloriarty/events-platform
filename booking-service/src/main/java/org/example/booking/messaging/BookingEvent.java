package org.example.booking.messaging;

import org.example.booking.model.OrdersModel;
import org.example.booking.model.RegistrationsModel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// Событие для других сервисов. Тот же контракт описан в notification-service (общего модуля нет).
// Адресат — владелец заказа или регистрации, а не тот, кто вызвал операцию: заказ может отменить ADMIN.
public record BookingEvent(
        String messageId,
        BookingEventType type,
        Long userId,
        Long orderId,
        BigDecimal totalPrice,
        Long registrationId,
        Long eventId,
        String eventTitle,
        LocalDateTime occurredAt
) {
    public static BookingEvent of(BookingEventType type, OrdersModel order) {
        return new BookingEvent(UUID.randomUUID().toString(), type, order.getUserId(),
                order.getId(), order.getTotalPrice(), null, null, null, LocalDateTime.now());
    }

    public static BookingEvent of(BookingEventType type, RegistrationsModel registration) {
        return new BookingEvent(UUID.randomUUID().toString(), type, registration.getUserId(),
                null, null, registration.getId(), registration.getEventId(),
                registration.getEventTitle(), LocalDateTime.now());
    }
}
