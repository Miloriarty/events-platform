package org.example.reminder.messaging;

import org.example.reminder.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReminderPublisher {

    private static final Logger log = LoggerFactory.getLogger(ReminderPublisher.class);
    private static final String ROUTING_KEY = "event.reminder";

    private final RabbitTemplate rabbit;

    public ReminderPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    // false: брокер недоступен, напоминание уйдет при следующей проверке
    public boolean publish(ReminderEvent event) {
        try {
            rabbit.convertAndSend(RabbitConfig.EXCHANGE, ROUTING_KEY, event);
            return true;
        } catch (AmqpException e) {
            log.warn("Failed to publish reminder {}: {}", event.messageId(), e.getMessage());
            return false;
        }
    }
}
