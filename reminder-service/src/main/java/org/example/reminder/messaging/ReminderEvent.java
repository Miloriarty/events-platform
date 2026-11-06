package org.example.reminder.messaging;

import org.example.reminder.client.EventInfo;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Читает notification-service, контракт описан там в BookingEvent (общего модуля нет)
public record ReminderEvent(
        String messageId,
        String type,
        Long userId,
        Long eventId,
        String eventTitle,
        LocalDate eventDate,
        LocalDateTime occurredAt
) {
    private static final String TYPE = "EVENT_REMINDER";

    // Id зависит только от адресата и мероприятия: notification-service отбрасывает повторы, поэтому
    // проверки можно запускать сколько угодно раз и своя БД не нужна. После переноса даты напоминание придет снова
    public static ReminderEvent of(EventInfo event, Long userId) {
        String messageId = "reminder:%d:%d:%s".formatted(event.id(), userId, event.startDate());
        return new ReminderEvent(messageId, TYPE, userId, event.id(), event.title(),
                event.startDate(), LocalDateTime.now());
    }
}
