package org.example.event.service;

import org.example.event.model.CategoriesModel;
import org.example.event.repository.CategoriesRepository;
import org.example.event.repository.EventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CategoriesServiceImpl implements CategoriesService {

    private final CategoriesRepository repository;
    private final EventRepository events;

    public CategoriesServiceImpl(CategoriesRepository repository, EventRepository events) {
        this.repository = repository;
        this.events = events;
    }

    @Override
    public List<CategoriesModel> findAll() {
        return repository.findAll();
    }

    @Override
    public CategoriesModel create(CategoriesModel category) {
        return repository.save(category);
    }

    @Override
    public CategoriesModel update(CategoriesModel category) {
        if (!repository.existsById(category.getId())) {
            throw notFound();
        }
        return repository.save(category);
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw notFound();
        }
        if (events.existsByCategoryId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Category is used by events and cannot be deleted");
        }
        repository.deleteById(id);
    }

    @Override
    public CategoriesModel findById(Long id) {
        return repository.findById(id).orElseThrow(this::notFound);
    }

    @Override
    public List<CategoriesModel> findByName(String name) {
        return repository.findByNameIgnoreCase(name);
    }

    @Override
    public Page<CategoriesModel> findPage(int page, int size) {
        return repository.findAll(PageRequest.of(page, size, Sort.by("id")));
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
    }
}