package org.example.booking.repository;

import org.example.booking.model.OrderItemsModel;
import org.example.booking.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemsRepository extends JpaRepository<OrderItemsModel, Long> {

    boolean existsByTicketId(Long ticketId);

    @Query("""
            select i from OrderItemsModel i join fetch i.order o
            where o.userId = :userId and o.status = :status
            order by o.orderDate desc, i.id""")
    List<OrderItemsModel> findByOwnerAndOrderStatus(@Param("userId") Long userId,
                                                    @Param("status") OrderStatus status);
}