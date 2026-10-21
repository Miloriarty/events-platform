package org.example.booking.repository;

import org.example.booking.model.RegistrationStatus;
import org.example.booking.model.RegistrationsModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistrationsRepository extends JpaRepository<RegistrationsModel, Long> {

    Page<RegistrationsModel> findByUserId(Long userId, Pageable pageable);

    List<RegistrationsModel> findByEventId(Long eventId);

    long countByEventIdAndStatus(Long eventId, RegistrationStatus status);

    long countByOrderItemIdAndStatus(Long orderItemId, RegistrationStatus status);

    @Query("select r from RegistrationsModel r where r.orderItem.order.id = :orderId")
    List<RegistrationsModel> findByOrderId(@Param("orderId") Long orderId);
}