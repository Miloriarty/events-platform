package org.example.event.service;

import org.example.event.model.LocationsModel;
import org.springframework.data.domain.Page;

import java.util.List;

public interface LocationsService {
    List<LocationsModel> findAll();
    LocationsModel create(LocationsModel location);
    LocationsModel update(LocationsModel location);
    void delete(Long id);
    LocationsModel findById(Long id);
    List<LocationsModel> findByName(String name);
    Page<LocationsModel> findPage(int page, int size);
}