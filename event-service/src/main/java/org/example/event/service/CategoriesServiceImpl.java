package org.example.event.service;

import org.example.event.model.CategoriesModel;
import org.example.event.repository.CategoriesRepository;
import org.example.event.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CategoriesServiceImpl implements CategoriesService {

    private static final Logger log = LoggerFactory.getLogger(CategoriesServiceImpl.class);

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
        CategoriesModel saved = repository.save(category);
        log.info("Created category '{}' (id={})", saved.getName(), saved.getId());
        return saved;
    }

    @Override
    public CategoriesModel update(CategoriesModel category) {
        if (!repository.existsById(category.getId())) {
            throw notFound();
        }
        CategoriesModel saved = repository.save(category);
        log.info("Updated category id={}", saved.getId());
        return saved;
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
        log.info("Deleted category id={}", id);
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