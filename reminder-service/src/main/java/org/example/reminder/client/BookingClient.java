package org.example.reminder.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

@HttpExchange("/api/registrations")
public interface BookingClient {

    @GetExchange("/event/{eventId}")
    List<RegistrationInfo> findByEvent(@PathVariable("eventId") Long eventId,
                                       @RequestHeader("X-User-Id") Long userId,
                                       @RequestHeader("X-User-Role") String role);
}
