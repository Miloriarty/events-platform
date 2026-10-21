package org.example.booking.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/api/events")
public interface EventClient {

    @GetExchange("/{id}")
    EventInfo getEvent(@PathVariable("id") Long id);
}