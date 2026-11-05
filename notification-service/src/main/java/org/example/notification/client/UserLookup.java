package org.example.notification.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Optional;

// Адрес почты принадлежит user-service, поэтому здесь он не хранится, а запрашивается перед отправкой
@Component
public class UserLookup {

    private static final Logger log = LoggerFactory.getLogger(UserLookup.class);

    private final UserClient client;
    private final CircuitBreaker breaker;

    public UserLookup(UserClient client, CircuitBreakerFactory<?, ?> factory) {
        this.client = client;
        this.breaker = factory.create("user-service");
    }

    // пусто: пользователь удалён или user-service недоступен
    public Optional<UserInfo> find(Long id) {
        return breaker.run(
                () -> fetch(id),
                throwable -> {
                    log.warn("Call to user-service failed for user {}: {}", id, throwable.toString());
                    return Optional.empty();
                });
    }

    private Optional<UserInfo> fetch(Long id) {
        try {
            return Optional.of(client.getUser(id));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }
}
