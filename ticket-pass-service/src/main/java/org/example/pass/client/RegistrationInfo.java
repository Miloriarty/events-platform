package org.example.pass.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RegistrationInfo(
        Long id,
        Long userId,
        Long eventId,
        String eventTitle,
        String ticketType,
        String status,
        LocalDateTime checkedInAt
) {
    public boolean isConfirmed() {
        return "CONFIRMED".equals(status);
    }
}
