package org.example.event.repository;

import org.example.event.model.LocationsModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationsRepository extends JpaRepository<LocationsModel, Long> {

    List<LocationsModel> findByNameIgnoreCase(String name);
}