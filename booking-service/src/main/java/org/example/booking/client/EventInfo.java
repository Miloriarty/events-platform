package org.example.booking.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventInfo(
        Long id,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        Long organizerId,
        Integer maxParticipants,
        BigDecimal minTicketPrice
) {
    // null: ответ event-service старой версии, ограничения еще нет
    public boolean accepts(BigDecimal ticketPrice) {
        return minTicketPrice == null || ticketPrice.compareTo(minTicketPrice) >= 0;
    }
}