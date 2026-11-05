package org.example.notification.messaging;

import org.example.notification.config.RabbitConfig;
import org.example.notification.service.NotificationsService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class BookingEventListener {

    private final NotificationsService notifications;

    public BookingEventListener(NotificationsService notifications) {
        this.notifications = notifications;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void onBookingEvent(BookingEvent event) {
        notifications.deliver(event);
    }
}
