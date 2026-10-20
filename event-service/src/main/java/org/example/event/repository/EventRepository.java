package org.example.event.repository;

import org.example.event.model.EventsModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<EventsModel, Long> {

    List<EventsModel> findByTitleContainingIgnoreCase(String title);

    List<EventsModel> findByOrganizerId(Long organizerId);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByLocationId(Long locationId);
}
