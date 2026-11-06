package org.example.reminder.client;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.time.LocalDate;
import java.util.List;

@HttpExchange("/api/events")
public interface EventClient {

    // Без @DateTimeFormat дата уходит в формате локали, и event-service отвечает 400
    @GetExchange("/starting")
    List<EventInfo> findStarting(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date);
}
