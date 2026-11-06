package org.example.notification.service;

import org.example.notification.messaging.BookingEvent;
import org.example.notification.model.NotificationsModel;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Текст уведомлений задает этот сервис: booking-service сообщает только о том, что произошло
@Component
public class NotificationTemplates {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private record Content(String title, String message, String link) {}

    public NotificationsModel build(BookingEvent event) {
        Content content = switch (event.type()) {
            case ORDER_CREATED -> new Content(
                    "Заказ #%d оформлен".formatted(event.orderId()),
                    "Сумма заказа: %s ₽.".formatted(event.totalPrice().toPlainString()),
                    "/orders");
            case ORDER_CANCELLED -> new Content(
                    "Заказ #%d отменён".formatted(event.orderId()),
                    "Билеты возвращены в продажу, регистрации по заказу отменены.",
                    "/orders");
            case REGISTRATION_CREATED -> new Content(
                    "Вы зарегистрированы на мероприятие",
                    "Мероприятие: «%s».".formatted(event.eventTitle()),
                    "/events/" + event.eventId());
            case REGISTRATION_CANCELLED -> new Content(
                    "Регистрация отменена",
                    "Мероприятие: «%s».".formatted(event.eventTitle()),
                    "/registrations");
            case EVENT_REMINDER -> new Content(
                    "Скоро мероприятие: «%s»".formatted(event.eventTitle()),
                    "Начало %s. Билет с QR-кодом можно открыть в разделе «Мои регистрации»."
                            .formatted(DATE.format(event.eventDate())),
                    "/registrations");
        };
        return new NotificationsModel(event.messageId(), event.userId(), event.type(),
                content.title(), content.message(), content.link(), LocalDateTime.now());
    }
}
