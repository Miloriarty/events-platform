package org.example.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.example.event.model.CategoriesModel;
import org.example.event.model.EventsModel;
import org.example.event.model.LocationsModel;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Мероприятие")
public record EventResponse(
        Long id,
        String title,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        LocationsModel location,
        CategoriesModel category,
        @Schema(description = "ID организатора (пользователь из user-service)") Long organizerId,
        Integer maxParticipants,
        @Schema(description = "Минимальная цена билета для регистрации; 0 — подходит любой билет")
        BigDecimal minTicketPrice,
        @Schema(description = "Адрес картинки; null, если картинки нет",
                example = "/api/events/1/image?v=1767225600000") String imageUrl
) {
    public static EventResponse from(EventsModel e) {
        String imageUrl = e.getImageVersion() == null ? null
                : "/api/events/" + e.getId() + "/image?v=" + e.getImageVersion();
        return new EventResponse(e.getId(), e.getTitle(), e.getDescription(),
                e.getStartDate(), e.getEndDate(), e.getLocation(), e.getCategory(),
                e.getOrganizerId(), e.getMaxParticipants(), e.getMinTicketPrice(), imageUrl);
    }
}