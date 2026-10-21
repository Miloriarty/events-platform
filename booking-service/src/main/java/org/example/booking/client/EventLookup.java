package org.example.booking.client;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class EventLookup {

    private final EventClient client;
    private final CircuitBreaker breaker;

    public EventLookup(EventClient client, CircuitBreakerFactory<?, ?> factory) {
        this.client = client;
        this.breaker = factory.create("event-service");
    }

    public EventInfo getEvent(Long id) {
        EventInfo event = breaker.run(
                () -> fetch(id),
                throwable -> {
                    // сюда попадаем при таймауте, ошибке связи и открытом выключателе
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "Event service is temporarily unavailable, please try again later");
                });
        if (event == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
        }
        return event;
    }

    // 404 от event-service это нормальный ответ, а не сбой: выключатель его не считает ошибкой
    private EventInfo fetch(Long id) {
        try {
            return client.getEvent(id);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }
}