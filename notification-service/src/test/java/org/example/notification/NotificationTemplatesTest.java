package org.example.notification;

import org.example.notification.messaging.BookingEvent;
import org.example.notification.model.NotificationType;
import org.example.notification.model.NotificationsModel;
import org.example.notification.service.NotificationTemplates;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationTemplatesTest {

    private final NotificationTemplates templates = new NotificationTemplates();

    private BookingEvent order(NotificationType type) {
        return new BookingEvent("m1", type, 7L, 12L, new BigDecimal("1500.00"),
                null, null, null, LocalDateTime.now());
    }

    private BookingEvent registration(NotificationType type) {
        return new BookingEvent("m2", type, 7L, null, null, 3L, 42L, "Java Meetup", LocalDateTime.now());
    }

    @Test
    void orderCreated() {
        NotificationsModel n = templates.build(order(NotificationType.ORDER_CREATED));
        assertEquals("Заказ #12 оформлен", n.getTitle());
        assertTrue(n.getMessage().contains("1500.00"));
        assertEquals("/orders", n.getLink());
        assertEquals(7L, n.getUserId());
    }

    @Test
    void registrationCreatedLinksToEvent() {
        NotificationsModel n = templates.build(registration(NotificationType.REGISTRATION_CREATED));
        assertEquals("/events/42", n.getLink());
        assertTrue(n.getMessage().contains("Java Meetup"));
    }

    @Test
    void everyTypeHasText() {
        for (NotificationType type : NotificationType.values()) {
            BookingEvent event = type.name().startsWith("ORDER") ? order(type) : registration(type);
            NotificationsModel n = templates.build(event);
            assertTrue(!n.getTitle().isBlank() && !n.getMessage().isBlank() && n.getLink().startsWith("/"));
        }
    }
}
