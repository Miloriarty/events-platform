package org.example.reminder.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
public class BookingLookup {

    private static final Logger log = LoggerFactory.getLogger(BookingLookup.class);

    // Системный вызов без пользователя. Как и остальные внутренние запросы, он идет мимо gateway
    private static final Long SYSTEM_USER_ID = 0L;
    private static final String SYSTEM_ROLE = "ADMIN";

    private final BookingClient client;
    private final CircuitBreaker breaker;

    public BookingLookup(BookingClient client, CircuitBreakerFactory<?, ?> factory) {
        this.client = client;
        this.breaker = factory.create("booking-service");
    }

    // id пользователей с подтвержденной регистрацией, каждый один раз: билетов у него может быть несколько
    public List<Long> findParticipants(Long eventId) {
        List<RegistrationInfo> registrations = breaker.run(
                () -> client.findByEvent(eventId, SYSTEM_USER_ID, SYSTEM_ROLE),
                throwable -> {
                    log.warn("Call to booking-service failed for event {}: {}", eventId, throwable.toString());
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "Booking service is temporarily unavailable, please try again later");
                });
        return registrations.stream()
                .filter(RegistrationInfo::isConfirmed)
                .map(RegistrationInfo::userId)
                .distinct()
                .toList();
    }
}
