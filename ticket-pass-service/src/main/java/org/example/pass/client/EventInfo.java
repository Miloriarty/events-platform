package org.example.pass.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventInfo(
        Long id,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        Location location
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Location(String name, String address, String city) {}
}
