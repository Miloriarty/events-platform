package org.example.booking.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventInfo(
        Long id,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        Long organizerId,
        Integer maxParticipants
) {}