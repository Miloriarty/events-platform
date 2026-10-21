package org.example.booking.service;

import org.example.booking.model.TicketsModel;
import org.example.booking.repository.OrderItemsRepository;
import org.example.booking.repository.TicketsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TicketsServiceImpl implements TicketsService {

    private final TicketsRepository tickets;
    private final OrderItemsRepository orderItems;

    public TicketsServiceImpl(TicketsRepository tickets, OrderItemsRepository orderItems) {
        this.tickets = tickets;
        this.orderItems = orderItems;
    }

    @Override
    public Page<TicketsModel> findPage(int page, int size) {
        return tickets.findAll(PageRequest.of(page, size, Sort.by("id")));
    }

    @Override
    public TicketsModel findById(Long id) {
        return tickets.findById(id).orElseThrow(this::notFound);
    }

    @Override
    public List<TicketsModel> findByType(String type) {
        return tickets.findByTypeIgnoreCase(type);
    }

    @Override
    public TicketsModel create(TicketsModel ticket) {
        return tickets.save(ticket);
    }

    @Override
    public TicketsModel update(TicketsModel ticket) {
        if (!tickets.existsById(ticket.getId())) {
            throw notFound();
        }
        return tickets.save(ticket);
    }

    @Override
    public void delete(Long id) {
        if (!tickets.existsById(id)) {
            throw notFound();
        }
        if (orderItems.existsByTicketId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ticket is used in orders and cannot be deleted");
        }
        tickets.deleteById(id);
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found");
    }
}