package org.example.event.service;

import org.example.event.dto.EventRequest;
import org.example.event.dto.EventResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface EventService {
    Page<EventResponse> findPage(int page, int size);
    EventResponse findById(Long id);
    List<EventResponse> searchByTitle(String title);
    List<EventResponse> findByOrganizer(Long organizerId);
    EventResponse create(EventRequest request, Long userId);
    EventResponse update(Long id, EventRequest request, Long userId, String role);
    void delete(Long id, Long userId, String role);
}
