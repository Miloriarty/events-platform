package org.example.booking.repository;

import org.example.booking.model.OrderItemsModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemsRepository extends JpaRepository<OrderItemsModel, Long> {

    boolean existsByTicketId(Long ticketId);
}