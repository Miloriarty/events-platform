package org.example.booking.service;

import org.example.booking.dto.OrderItemRequest;
import org.example.booking.dto.OrderRequest;
import org.example.booking.dto.OrderResponse;
import org.example.booking.messaging.BookingEvent;
import org.example.booking.messaging.BookingEventType;
import org.example.booking.model.OrderItemsModel;
import org.example.booking.model.OrderStatus;
import org.example.booking.model.OrdersModel;
import org.example.booking.model.RegistrationStatus;
import org.example.booking.model.TicketsModel;
import org.example.booking.repository.OrdersRepository;
import org.example.booking.repository.RegistrationsRepository;
import org.example.booking.repository.TicketsRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class OrdersServiceImpl implements OrdersService {

    private static final Logger log = LoggerFactory.getLogger(OrdersServiceImpl.class);

    private final OrdersRepository orders;
    private final TicketsRepository tickets;
    private final RegistrationsRepository registrations;
    private final ApplicationEventPublisher events;

    public OrdersServiceImpl(OrdersRepository orders, TicketsRepository tickets,
                             RegistrationsRepository registrations,
                             ApplicationEventPublisher events) {
        this.orders = orders;
        this.tickets = tickets;
        this.registrations = registrations;
        this.events = events;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> findPage(int page, int size, Long userId, String role) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "orderDate"));
        Page<OrdersModel> result = isAdmin(role)
                ? orders.findAll(pageable)
                : orders.findByUserId(userId, pageable);
        return result.map(OrderResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse findById(Long id, Long userId, String role) {
        return OrderResponse.from(getVisible(id, userId, role));
    }

    @Override
    @Transactional
    public OrderResponse create(OrderRequest request, Long userId) {
        OrdersModel order = new OrdersModel();
        order.setUserId(userId);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.CREATED);

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest line : request.items()) {
            TicketsModel ticket = tickets.findById(line.ticketId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Ticket not found: " + line.ticketId()));

            // атомарное списание; при нехватке вся транзакция откатывается
            if (tickets.reserve(ticket.getId(), line.quantity()) == 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Not enough tickets of type " + ticket.getType());
            }

            OrderItemsModel item = new OrderItemsModel();
            item.setTicket(ticket);
            item.setTicketType(ticket.getType());
            item.setQuantity(line.quantity());
            item.setUnitPrice(ticket.getPrice());      // цена берется из БД, а не от клиента
            order.addItem(item);

            total = total.add(ticket.getPrice().multiply(BigDecimal.valueOf(line.quantity())));
        }
        order.setTotalPrice(total);
        OrdersModel saved = orders.save(order);
        log.info("Created order id={} for user {}: {} item(s), total {}",
                saved.getId(), userId, saved.getItems().size(), total);
        events.publishEvent(BookingEvent.of(BookingEventType.ORDER_CREATED, saved));
        return OrderResponse.from(saved);
    }

    @Override
    @Transactional
    public OrderResponse cancel(Long id, Long userId, String role) {
        OrdersModel order = getVisible(id, userId, role);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return OrderResponse.from(order);          // повторная отмена безвредна
        }
        order.setStatus(OrderStatus.CANCELLED);
        for (OrderItemsModel item : order.getItems()) {
            tickets.release(item.getTicket().getId(), item.getQuantity());
        }
        registrations.findByOrderId(order.getId())
                .forEach(r -> r.setStatus(RegistrationStatus.CANCELLED));
        log.info("Cancelled order id={} by user {} ({}), tickets returned to stock", id, userId, role);
        events.publishEvent(BookingEvent.of(BookingEventType.ORDER_CANCELLED, order));
        return OrderResponse.from(order);
    }

    // чужой заказ отвечает 404, а не 403: не подсказываем, что такой id существует
    private OrdersModel getVisible(Long id, Long userId, String role) {
        return orders.findById(id)
                .filter(o -> isAdmin(role) || o.getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private boolean isAdmin(String role) {
        return "ADMIN".equals(role);
    }
}