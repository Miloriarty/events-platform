package org.example.booking.repository;

import org.example.booking.model.TicketsModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketsRepository extends JpaRepository<TicketsModel, Long> {

    List<TicketsModel> findByTypeIgnoreCase(String type);

    // Атомарное списание: вернёт 0, если билетов не хватает
    @Modifying
    @Query("update TicketsModel t set t.quantity = t.quantity - :amount "
            + "where t.id = :id and t.quantity >= :amount")
    int reserve(@Param("id") Long id, @Param("amount") int amount);

    // Возврат билетов при отмене заказа
    @Modifying
    @Query("update TicketsModel t set t.quantity = t.quantity + :amount where t.id = :id")
    int release(@Param("id") Long id, @Param("amount") int amount);
}