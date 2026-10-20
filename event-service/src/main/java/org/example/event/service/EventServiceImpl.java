package org.example.event.service;

import org.example.event.dto.EventRequest;
import org.example.event.dto.EventResponse;
import org.example.event.model.CategoriesModel;
import org.example.event.model.EventsModel;
import org.example.event.model.LocationsModel;
import org.example.event.repository.CategoriesRepository;
import org.example.event.repository.EventRepository;
import org.example.event.repository.LocationsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository events;
    private final LocationsRepository locations;
    private final CategoriesRepository categories;

    public EventServiceImpl(EventRepository events, LocationsRepository locations,
                            CategoriesRepository categories) {
        this.events = events;
        this.locations = locations;
        this.categories = categories;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponse> findPage(int page, int size) {
        return events.findAll(PageRequest.of(page, size, Sort.by("startDate")))
                .map(EventResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse findById(Long id) {
        return EventResponse.from(getOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> searchByTitle(String title) {
        return events.findByTitleContainingIgnoreCase(title).stream()
                .map(EventResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> findByOrganizer(Long organizerId) {
        return events.findByOrganizerId(organizerId).stream()
                .map(EventResponse::from).toList();
    }

    @Override
    @Transactional
    public EventResponse create(EventRequest request, Long userId) {
        EventsModel event = new EventsModel();
        event.setOrganizerId(userId);
        apply(event, request);
        return EventResponse.from(events.save(event));
    }

    @Override
    @Transactional
    public EventResponse update(Long id, EventRequest request, Long userId, String role) {
        EventsModel event = getOrThrow(id);
        checkCanModify(event, userId, role);
        apply(event, request);                 // organizerId не меняется
        return EventResponse.from(events.save(event));
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId, String role) {
        EventsModel event = getOrThrow(id);
        checkCanModify(event, userId, role);
        events.delete(event);
    }

    private void apply(EventsModel event, EventRequest r) {
        if (r.endDate().isBefore(r.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "End date must not be before start date");
        }
        LocationsModel location = locations.findById(r.locationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Location not found"));
        CategoriesModel category = categories.findById(r.categoryId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Category not found"));

        int capacity = location.getCapacity();
        int max = r.maxParticipants() != null ? r.maxParticipants() : capacity;
        if (max > capacity) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Max participants (" + max + ") exceeds location capacity (" + capacity + ")");
        }

        event.setTitle(r.title());
        event.setDescription(r.description());
        event.setStartDate(r.startDate());
        event.setEndDate(r.endDate());
        event.setLocation(location);
        event.setCategory(category);
        event.setMaxParticipants(max);
    }

    private void checkCanModify(EventsModel event, Long userId, String role) {
        boolean admin = "ADMIN".equals(role);
        if (!admin && !event.getOrganizerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the organizer or an admin can modify this event");
        }
    }

    private EventsModel getOrThrow(Long id) {
        return events.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Event not found"));
    }
}