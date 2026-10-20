package org.example.event.service;

import org.example.event.model.CategoriesModel;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CategoriesService {
    List<CategoriesModel> findAll();
    CategoriesModel create(CategoriesModel category);
    CategoriesModel update(CategoriesModel category);
    void delete(Long id);
    CategoriesModel findById(Long id);
    List<CategoriesModel> findByName(String name);
    Page<CategoriesModel> findPage(int page, int size);
}