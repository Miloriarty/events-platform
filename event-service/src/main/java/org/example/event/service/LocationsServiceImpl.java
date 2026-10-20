package org.example.event.service;

import org.example.event.model.LocationsModel;
import org.example.event.repository.EventRepository;
import org.example.event.repository.LocationsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class LocationsServiceImpl implements LocationsService {

    private final LocationsRepository repository;
    private final EventRepository events;

    public LocationsServiceImpl(LocationsRepository repository, EventRepository events) {
        this.repository = repository;
        this.events = events;
    }

    @Override
    public List<LocationsModel> findAll() {
        return repository.findAll();
    }

    @Override
    public LocationsModel create(LocationsModel location) {
        return repository.save(location);
    }

    @Override
    public LocationsModel update(LocationsModel location) {
        if (!repository.existsById(location.getId())) {
            throw notFound();
        }
        return repository.save(location);
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw notFound();
        }
        if (events.existsByLocationId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Location is used by events and cannot be deleted");
        }
        repository.deleteById(id);
    }

    @Override
    public LocationsModel findById(Long id) {
        return repository.findById(id).orElseThrow(this::notFound);
    }

    @Override
    public List<LocationsModel> findByName(String name) {
        return repository.findByNameIgnoreCase(name);
    }

    @Override
    public Page<LocationsModel> findPage(int page, int size) {
        return repository.findAll(PageRequest.of(page, size, Sort.by("id")));
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found");
    }
}