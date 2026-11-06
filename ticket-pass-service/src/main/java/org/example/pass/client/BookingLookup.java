package org.example.pass.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.function.Supplier;

@Component
public class BookingLookup {

    private static final Logger log = LoggerFactory.getLogger(BookingLookup.class);

    // Ответ 4xx - нормальный отказ (чужая регистрация, билет уже использован), брейкеру он не засчитывается
    private record Outcome(RegistrationInfo registration, HttpClientErrorException rejection) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ErrorBody(String detail) {}

    private final BookingClient client;
    private final CircuitBreaker breaker;

    public BookingLookup(BookingClient client, CircuitBreakerFactory<?, ?> factory) {
        this.client = client;
        this.breaker = factory.create("booking-service");
    }

    public RegistrationInfo getRegistration(Long id, Long userId, String role) {
        return call(() -> client.getRegistration(id, userId, role));
    }

    public RegistrationInfo checkIn(Long id, Long userId, String role) {
        return call(() -> client.checkIn(id, userId, role));
    }

    private RegistrationInfo call(Supplier<RegistrationInfo> request) {
        Outcome outcome = breaker.run(
                () -> {
                    try {
                        return new Outcome(request.get(), null);
                    } catch (HttpClientErrorException e) {
                        return new Outcome(null, e);
                    }
                },
                throwable -> {
                    log.warn("Call to booking-service failed", throwable);
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "Booking service is temporarily unavailable, please try again later");
                });
        if (outcome.rejection() != null) {
            throw new ResponseStatusException(outcome.rejection().getStatusCode(), detail(outcome.rejection()));
        }
        return outcome.registration();
    }

    // клиент должен увидеть причину отказа от booking-service, а не общий текст
    private String detail(HttpClientErrorException e) {
        try {
            ErrorBody body = e.getResponseBodyAs(ErrorBody.class);
            return body != null && body.detail() != null ? body.detail() : e.getStatusText();
        } catch (RestClientException | IllegalStateException unreadable) {
            return e.getStatusText();
        }
    }
}
