package org.example.event.repository;

import org.example.event.model.EventImagesModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventImagesRepository extends JpaRepository<EventImagesModel, Long> {
}
