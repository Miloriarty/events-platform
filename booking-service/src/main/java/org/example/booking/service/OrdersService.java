package org.example.booking.service;

import org.example.booking.dto.OrderRequest;
import org.example.booking.dto.OrderResponse;
import org.springframework.data.domain.Page;

public interface OrdersService {
    Page<OrderResponse> findPage(int page, int size, Long userId, String role);
    OrderResponse findById(Long id, Long userId, String role);
    OrderResponse create(OrderRequest request, Long userId);
    OrderResponse cancel(Long id, Long userId, String role);
}