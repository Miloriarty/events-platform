package org.example.event.repository;

import org.example.event.model.CategoriesModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoriesRepository extends JpaRepository<CategoriesModel, Long> {

    List<CategoriesModel> findByNameIgnoreCase(String name);
}