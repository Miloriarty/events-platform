package org.example.reminder.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "reminder.events";

    // Очередь объявляет потребитель: издателю не нужно знать, кто его слушает
    @Bean
    TopicExchange reminderExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
