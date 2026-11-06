package org.example.event.repository;

import org.example.event.model.EventsModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<EventsModel, Long> {

    List<EventsModel> findByTitleContainingIgnoreCase(String title);

    List<EventsModel> findByOrganizerId(Long organizerId);

    List<EventsModel> findByStartDate(LocalDate startDate);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByLocationId(Long locationId);
}
