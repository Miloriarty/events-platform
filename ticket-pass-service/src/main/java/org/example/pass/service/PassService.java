package org.example.pass.service;

import org.example.pass.dto.CheckInResponse;
import org.example.pass.dto.PassResponse;

public interface PassService {

    // билет своей подтвержденной регистрации; ADMIN может получить любой
    PassResponse getPass(Long registrationId, Long userId, String role);

    byte[] getPdf(Long registrationId, Long userId, String role);

    // проверяет подпись токена и отмечает проход в booking-service
    CheckInResponse checkIn(String token, Long userId, String role);
}
