package org.example.booking.service;

import org.example.booking.client.EventInfo;
import org.example.booking.client.EventLookup;
import org.example.booking.dto.RegistrationRequest;
import org.example.booking.dto.RegistrationResponse;
import org.example.booking.model.OrderItemsModel;
import org.example.booking.model.OrderStatus;
import org.example.booking.model.RegistrationStatus;
import org.example.booking.model.RegistrationsModel;
import org.example.booking.repository.OrderItemsRepository;
import org.example.booking.repository.RegistrationsRepository;
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

    private final RegistrationsRepository registrations;
    private final OrderItemsRepository orderItems;
    private final EventLookup eventLookup;

    public RegistrationsServiceImpl(RegistrationsRepository registrations,
                                    OrderItemsRepository orderItems,
                                    EventLookup eventLookup) {
        this.registrations = registrations;
        this.orderItems = orderItems;
        this.eventLookup = eventLookup;
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
        if (!isAdmin(role)) {
            EventInfo event = eventLookup.getEvent(eventId);
            if (!event.organizerId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Only the organizer or an admin can view participants");
            }
        }
        return registrations.findByEventId(eventId).stream()
                .map(RegistrationResponse::from).toList();
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
        long usedSeats = registrations.countByOrderItemIdAndStatus(item.getId(), RegistrationStatus.CONFIRMED);
        if (usedSeats >= item.getQuantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "All tickets of this order item are already used");
        }

        EventInfo event = eventLookup.getEvent(request.eventId());
        if (event.endDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event has already finished");
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
        return RegistrationResponse.from(registrations.save(registration));
    }

    @Override
    @Transactional
    public RegistrationResponse cancel(Long id, Long userId, String role) {
        RegistrationsModel registration = getVisible(id, userId, role);
        registration.setStatus(RegistrationStatus.CANCELLED);
        return RegistrationResponse.from(registration);
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