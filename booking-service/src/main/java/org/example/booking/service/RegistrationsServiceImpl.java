package org.example.booking.service;

import org.example.booking.client.EventInfo;
import org.example.booking.client.EventLookup;
import org.example.booking.dto.AvailableOrderItem;
import org.example.booking.dto.RegistrationRequest;
import org.example.booking.dto.RegistrationResponse;
import org.example.booking.messaging.BookingEvent;
import org.example.booking.messaging.BookingEventType;
import org.example.booking.model.OrderItemsModel;
import org.example.booking.model.OrderStatus;
import org.example.booking.model.RegistrationStatus;
import org.example.booking.model.RegistrationsModel;
import org.example.booking.repository.OrderItemsRepository;
import org.example.booking.repository.RegistrationsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RegistrationsServiceImpl implements RegistrationsService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationsServiceImpl.class);

    private final RegistrationsRepository registrations;
    private final OrderItemsRepository orderItems;
    private final EventLookup eventLookup;
    private final ApplicationEventPublisher events;

    public RegistrationsServiceImpl(RegistrationsRepository registrations,
                                    OrderItemsRepository orderItems,
                                    EventLookup eventLookup,
                                    ApplicationEventPublisher events) {
        this.registrations = registrations;
        this.orderItems = orderItems;
        this.eventLookup = eventLookup;
        this.events = events;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RegistrationResponse> findPage(int page, int size, Long userId, String role) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "registrationDate"));
        Page<RegistrationsModel> result = isAdmin(role)
                ? registrations.findAll(pageable)
                : registrations.findByUserId(userId, pageable);
        return result.map(RegistrationResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public RegistrationResponse findById(Long id, Long userId, String role) {
        return RegistrationResponse.from(getVisible(id, userId, role));
    }

    // участников мероприятия видит его организатор или ADMIN
    @Override
    @Transactional(readOnly = true)
    public List<RegistrationResponse> findByEvent(Long eventId, Long userId, String role) {
        requireOrganizer(eventId, userId, role, "Only the organizer or an admin can view participants");
        return registrations.findByEventId(eventId).stream()
                .map(RegistrationResponse::from).toList();
    }

    // Только свои позиции и для ADMIN: регистрация по чужому заказу запрещена всем
    @Override
    @Transactional(readOnly = true)
    public List<AvailableOrderItem> findAvailableItems(Long eventId, Long userId) {
        EventInfo event = eventLookup.getEvent(eventId);
        return orderItems.findByOwnerAndOrderStatus(userId, OrderStatus.CREATED).stream()
                .filter(item -> event.accepts(item.getUnitPrice()))
                .map(item -> AvailableOrderItem.from(item, item.getQuantity() - usedSeats(item)))
                .filter(item -> item.remaining() > 0)
                .toList();
    }

    @Override
    @Transactional
    public RegistrationResponse create(RegistrationRequest request, Long userId) {
        OrderItemsModel item = orderItems.findById(request.orderItemId())
                .filter(i -> i.getOrder().getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order item not found"));

        if (item.getOrder().getStatus() != OrderStatus.CREATED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order is cancelled");
        }
        if (usedSeats(item) >= item.getQuantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "All tickets of this order item are already used");
        }

        EventInfo event = eventLookup.getEvent(request.eventId());
        if (event.endDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event has already finished");
        }
        // сравнивается цена покупки: изменение цены типа билета не влияет на купленные билеты
        if (!event.accepts(item.getUnitPrice())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ticket price is below the minimum for this event");
        }
        long taken = registrations.countByEventIdAndStatus(event.id(), RegistrationStatus.CONFIRMED);
        if (taken >= event.maxParticipants()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Event is fully booked");
        }

        RegistrationsModel registration = new RegistrationsModel();
        registration.setUserId(userId);
        registration.setEventId(event.id());
        registration.setEventTitle(event.title());     // копия: мероприятие могут потом удалить
        registration.setOrderItem(item);
        registration.setRegistrationDate(LocalDateTime.now());
        registration.setStatus(RegistrationStatus.CONFIRMED);
        RegistrationsModel saved = registrations.save(registration);
        log.info("Registered user {} for event {} (registration id={}, order item {})",
                userId, event.id(), saved.getId(), item.getId());
        events.publishEvent(BookingEvent.of(BookingEventType.REGISTRATION_CREATED, saved));
        return RegistrationResponse.from(saved);
    }

    @Override
    @Transactional
    public RegistrationResponse cancel(Long id, Long userId, String role) {
        RegistrationsModel registration = getVisible(id, userId, role);
        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            return RegistrationResponse.from(registration);     // повторная отмена безвредна
        }
        registration.setStatus(RegistrationStatus.CANCELLED);
        log.info("Cancelled registration id={} by user {} ({})", id, userId, role);
        events.publishEvent(BookingEvent.of(BookingEventType.REGISTRATION_CANCELLED, registration));
        return RegistrationResponse.from(registration);
    }

    // пропускает на мероприятие его организатор или ADMIN
    @Override
    @Transactional
    public RegistrationResponse checkIn(Long id, Long userId, String role) {
        RegistrationsModel registration = registrations.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));
        requireOrganizer(registration.getEventId(), userId, role,
                "Only the organizer or an admin can check in participants");
        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Registration is cancelled");
        }
        if (registrations.markCheckedIn(id, LocalDateTime.now()) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ticket has already been used");
        }
        log.info("Checked in registration id={} by user {} ({})", id, userId, role);
        return registrations.findById(id).map(RegistrationResponse::from).orElseThrow();
    }

    private long usedSeats(OrderItemsModel item) {
        return registrations.countByOrderItemIdAndStatus(item.getId(), RegistrationStatus.CONFIRMED);
    }

    private void requireOrganizer(Long eventId, Long userId, String role, String deniedMessage) {
        if (isAdmin(role)) {
            return;
        }
        EventInfo event = eventLookup.getEvent(eventId);
        if (!event.organizerId().equals(userId)) {
            log.warn("Access denied: user {} is not the organizer of event {}", userId, eventId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, deniedMessage);
        }
    }

    private RegistrationsModel getVisible(Long id, Long userId, String role) {
        return registrations.findById(id)
                .filter(r -> isAdmin(role) || r.getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));
    }

    private boolean isAdmin(String role) {
        return "ADMIN".equals(role);
    }
}