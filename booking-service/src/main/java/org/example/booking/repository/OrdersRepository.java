package org.example.booking.repository;

import org.example.booking.model.OrdersModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrdersRepository extends JpaRepository<OrdersModel, Long> {

    Page<OrdersModel> findByUserId(Long userId, Pageable pageable);
}