package org.example.reminder.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RegistrationInfo(Long userId, String status) {

    public boolean isConfirmed() {
        return "CONFIRMED".equals(status);
    }
}
