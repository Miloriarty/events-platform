package org.example.booking.service;

import org.example.booking.dto.RegistrationRequest;
import org.example.booking.dto.RegistrationResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface RegistrationsService {
    Page<RegistrationResponse> findPage(int page, int size, Long userId, String role);
    RegistrationResponse findById(Long id, Long userId, String role);
    List<RegistrationResponse> findByEvent(Long eventId, Long userId, String role);
    RegistrationResponse create(RegistrationRequest request, Long userId);
    RegistrationResponse cancel(Long id, Long userId, String role);
}