package org.example.reminder.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Component
public class EventLookup {

    private static final Logger log = LoggerFactory.getLogger(EventLookup.class);

    private final EventClient client;
    private final CircuitBreaker breaker;

    public EventLookup(EventClient client, CircuitBreakerFactory<?, ?> factory) {
        this.client = client;
        this.breaker = factory.create("event-service");
    }

    public List<EventInfo> findStarting(LocalDate date) {
        return breaker.run(
                () -> client.findStarting(date),
                throwable -> {
                    log.warn("Call to event-service failed for date {}: {}", date, throwable.toString());
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "Event service is temporarily unavailable, please try again later");
                });
    }
}
