package org.example.user.service;

import org.example.user.model.RolesModel;
import org.example.user.repository.RolesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RolesServiceImpl implements RolesService {

    private static final Logger log = LoggerFactory.getLogger(RolesServiceImpl.class);

    private final RolesRepository repository;

    public RolesServiceImpl(RolesRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<RolesModel> findAll() {
        return repository.findAll();
    }

    @Override
    public RolesModel create(RolesModel role) {
        RolesModel saved = repository.save(role);
        log.info("Created role '{}' (id={})", saved.getName(), saved.getId());
        return saved;
    }

    @Override
    public RolesModel update(RolesModel role) {
        if (!repository.existsById(role.getId())) {
            throw new RuntimeException("Role not found");
        }
        RolesModel saved = repository.save(role);
        log.info("Updated role id={}", saved.getId());
        return saved;
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
        log.info("Deleted role id={}", id);
    }

    @Override
    public RolesModel findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found"));
    }

    @Override
    public List<RolesModel> findByName(String name) {
        return repository.findByNameIgnoreCase(name);
    }

    @Override
    public Page<RolesModel> findPage(int page, int size) {
        return repository.findAll(PageRequest.of(page, size));
    }
}