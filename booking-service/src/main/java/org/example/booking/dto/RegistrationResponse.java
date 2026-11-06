package org.example.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.example.booking.model.RegistrationStatus;
import org.example.booking.model.RegistrationsModel;

import java.time.LocalDateTime;

@Schema(description = "Регистрация на мероприятие")
public record RegistrationResponse(
        Long id,
        Long userId,
        Long eventId,
        @Schema(description = "Название мероприятия на момент регистрации") String eventTitle,
        Long orderItemId,
        @Schema(description = "Тип билета на момент покупки") String ticketType,
        LocalDateTime registrationDate,
        RegistrationStatus status,
        @Schema(description = "Время прохода на мероприятие; null, если билет не использован")
        LocalDateTime checkedInAt
) {
    public static RegistrationResponse from(RegistrationsModel r) {
        return new RegistrationResponse(r.getId(), r.getUserId(), r.getEventId(), r.getEventTitle(),
                r.getOrderItem().getId(), r.getOrderItem().getTicketType(),
                r.getRegistrationDate(), r.getStatus(), r.getCheckedInAt());
    }
}