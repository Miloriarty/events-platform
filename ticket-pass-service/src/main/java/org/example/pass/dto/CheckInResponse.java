package org.example.pass.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.example.pass.client.RegistrationInfo;

import java.time.LocalDateTime;

@Schema(description = "Результат проверки билета")
public record CheckInResponse(
        Long registrationId,
        Long userId,
        Long eventId,
        String eventTitle,
        String ticketType,
        LocalDateTime checkedInAt
) {
    public static CheckInResponse from(RegistrationInfo r) {
        return new CheckInResponse(r.id(), r.userId(), r.eventId(), r.eventTitle(), r.ticketType(), r.checkedInAt());
    }
}
