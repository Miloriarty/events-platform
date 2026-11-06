package org.example.event.service;

import org.example.event.dto.EventImage;
import org.example.event.dto.EventRequest;
import org.example.event.dto.EventResponse;
import org.example.event.model.CategoriesModel;
import org.example.event.model.EventImagesModel;
import org.example.event.model.EventsModel;
import org.example.event.model.LocationsModel;
import org.example.event.repository.CategoriesRepository;
import org.example.event.repository.EventImagesRepository;
import org.example.event.repository.EventRepository;
import org.example.event.repository.LocationsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class EventServiceImpl implements EventService {

    private static final Logger log = LoggerFactory.getLogger(EventServiceImpl.class);

    private final EventRepository events;
    private final LocationsRepository locations;
    private final CategoriesRepository categories;
    private final EventImagesRepository images;

    // SVG не принимаем: он может содержать скрипты
    private static final Set<String> IMAGE_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    static final long MAX_IMAGE_BYTES = 5 * 1024 * 1024;

    public EventServiceImpl(EventRepository events, LocationsRepository locations,
                            CategoriesRepository categories, EventImagesRepository images) {
        this.events = events;
        this.locations = locations;
        this.categories = categories;
        this.images = images;
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
    public List<EventResponse> findByStartDate(LocalDate date) {
        return events.findByStartDate(date).stream().map(EventResponse::from).toList();
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
        checkStartNotInPast(request.startDate());
        apply(event, request);
        EventsModel saved = events.save(event);
        log.info("Created event '{}' (id={}) by organizer {}", saved.getTitle(), saved.getId(), userId);
        return EventResponse.from(saved);
    }

    @Override
    @Transactional
    public EventResponse update(Long id, EventRequest request, Long userId, String role) {
        EventsModel event = getOrThrow(id);
        checkCanModify(event, userId, role);
        // уже прошедшее мероприятие можно сохранить с прежней датой, но перенести в прошлое нельзя
        if (!request.startDate().equals(event.getStartDate())) {
            checkStartNotInPast(request.startDate());
        }
        apply(event, request);                 // organizerId не меняется
        EventsModel saved = events.save(event);
        log.info("Updated event id={} by user {} ({})", id, userId, role);
        return EventResponse.from(saved);
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId, String role) {
        EventsModel event = getOrThrow(id);
        checkCanModify(event, userId, role);
        images.deleteById(id);
        events.delete(event);
        log.info("Deleted event id={} by user {} ({})", id, userId, role);
    }

    @Override
    @Transactional(readOnly = true)
    public EventImage getImage(Long id) {
        return images.findById(id)
                .map(i -> new EventImage(i.getContentType(), i.getData()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"));
    }

    @Override
    @Transactional
    public EventResponse uploadImage(Long id, String contentType, byte[] data, Long userId, String role) {
        EventsModel event = getOrThrow(id);
        checkCanModify(event, userId, role);
        if (contentType == null || !IMAGE_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only JPEG, PNG, WebP and GIF images are allowed");
        }
        if (data.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is empty");
        }
        if (data.length > MAX_IMAGE_BYTES) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "Image must not exceed 5 MB");
        }
        EventImagesModel image = images.findById(id).orElseGet(() -> new EventImagesModel(id, contentType, data));
        image.setContentType(contentType);
        image.setData(data);
        images.save(image);
        event.setImageVersion(System.currentTimeMillis());
        EventsModel saved = events.save(event);
        log.info("Uploaded image for event id={} ({}, {} bytes) by user {} ({})",
                id, contentType, data.length, userId, role);
        return EventResponse.from(saved);
    }

    @Override
    @Transactional
    public EventResponse deleteImage(Long id, Long userId, String role) {
        EventsModel event = getOrThrow(id);
        checkCanModify(event, userId, role);
        images.deleteById(id);
        event.setImageVersion(null);
        EventsModel saved = events.save(event);
        log.info("Deleted image of event id={} by user {} ({})", id, userId, role);
        return EventResponse.from(saved);
    }

    private void checkStartNotInPast(LocalDate startDate) {
        if (startDate.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Start date must not be in the past");
        }
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
        event.setMinTicketPrice(r.minTicketPrice());
    }

    private void checkCanModify(EventsModel event, Long userId, String role) {
        boolean admin = "ADMIN".equals(role);
        if (!admin && !event.getOrganizerId().equals(userId)) {
            log.warn("User {} ({}) tried to modify event {} of organizer {}",
                    userId, role, event.getId(), event.getOrganizerId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the organizer or an admin can modify this event");
        }
    }

    private EventsModel getOrThrow(Long id) {
        return events.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Event not found"));
    }
}