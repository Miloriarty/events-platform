package org.example.pass.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

// Заголовки пользователя передаются дальше: права проверяет booking-service, он владеет регистрациями
@HttpExchange("/api/registrations")
public interface BookingClient {

    @GetExchange("/{id}")
    RegistrationInfo getRegistration(@PathVariable("id") Long id,
                                     @RequestHeader("X-User-Id") Long userId,
                                     @RequestHeader("X-User-Role") String role);

    @PostExchange("/{id}/check-in")
    RegistrationInfo checkIn(@PathVariable("id") Long id,
                             @RequestHeader("X-User-Id") Long userId,
                             @RequestHeader("X-User-Role") String role);
}
