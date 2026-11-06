package org.example.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "booking.events";
    public static final String REMINDER_EXCHANGE = "reminder.events";
    public static final String QUEUE = "notification.booking-events";
    public static final String DEAD_LETTER_QUEUE = QUEUE + ".dlq";

    @Bean
    TopicExchange bookingExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    TopicExchange reminderExchange() {
        return new TopicExchange(REMINDER_EXCHANGE);
    }

    // Сообщение, которое не удалось обработать за все попытки, уходит в DLQ, а не теряется
    @Bean
    Queue bookingEventsQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    Queue bookingEventsDeadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding bookingEventsBinding(Queue bookingEventsQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(bookingEventsQueue).to(bookingExchange).with("#");
    }

    @Bean
    Binding reminderEventsBinding(Queue bookingEventsQueue, TopicExchange reminderExchange) {
        return BindingBuilder.bind(bookingEventsQueue).to(reminderExchange).with("#");
    }

    // Класс определяется по параметру слушателя: в заголовке __TypeId__ лежит класс из booking-service
    @Bean
    MessageConverter messageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }
}
