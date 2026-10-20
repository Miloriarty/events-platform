package org.example.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.example.event.model.CategoriesModel;
import org.example.event.model.EventsModel;
import org.example.event.model.LocationsModel;

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
        Integer maxParticipants
) {
    public static EventResponse from(EventsModel e) {
        return new EventResponse(e.getId(), e.getTitle(), e.getDescription(),
                e.getStartDate(), e.getEndDate(), e.getLocation(), e.getCategory(),
                e.getOrganizerId(), e.getMaxParticipants());
    }
}