package org.example.user.service;

import org.example.user.model.RolesModel;
import org.springframework.data.domain.Page;

import java.util.List;

public interface RolesService {

    List<RolesModel> findAll();

    RolesModel create(RolesModel role);

    RolesModel update(RolesModel role);

    void delete(Long id);

    RolesModel findById(Long id);

    List<RolesModel> findByName(String name);

    Page<RolesModel> findPage(int page, int size);
}