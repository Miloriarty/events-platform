package org.example.booking.messaging;

import org.example.booking.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BookingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(BookingEventPublisher.class);

    private final RabbitTemplate rabbit;

    public BookingEventPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    // После коммита: об откатившемся заказе никто не узнает.
    // Сбой брокера не должен ломать заказ, поэтому уведомление в этом случае теряется
    @TransactionalEventListener
    public void publish(BookingEvent event) {
        try {
            rabbit.convertAndSend(RabbitConfig.EXCHANGE, event.type().routingKey(), event);
            log.info("Published {} (message {}) for user {}", event.type(), event.messageId(), event.userId());
        } catch (AmqpException e) {
            log.warn("Failed to publish {} for user {}: {}", event.type(), event.userId(), e.getMessage());
        }
    }
}
