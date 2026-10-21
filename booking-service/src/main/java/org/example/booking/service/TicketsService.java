package org.example.booking.service;

import org.example.booking.model.TicketsModel;
import org.springframework.data.domain.Page;

import java.util.List;

public interface TicketsService {
    Page<TicketsModel> findPage(int page, int size);
    TicketsModel findById(Long id);
    List<TicketsModel> findByType(String type);
    TicketsModel create(TicketsModel ticket);
    TicketsModel update(TicketsModel ticket);
    void delete(Long id);
}