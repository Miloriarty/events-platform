package org.example.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.event.model.CategoriesModel;
import org.example.event.service.CategoriesService;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Категории", description = "API для работы с категориями")
@SecurityRequirement(name = "BearerAuth")
public class CategoriesController {

    private final CategoriesService categoriesService;

    public CategoriesController(CategoriesService categoriesService) {
        this.categoriesService = categoriesService;
    }

    @Operation(summary = "Получить все категории", description = "Список постранично")
    @GetMapping
    public PagedModel<CategoriesModel> getAll(
            @RequestParam(defaultValue = "0") @Parameter(description = "Номер страницы") int page,
            @RequestParam(defaultValue = "5") @Parameter(description = "Размер страницы") int size) {
        return new PagedModel<>(categoriesService.findPage(page, size));
    }

    @Operation(summary = "Получить категорию по ID")
    @ApiResponse(responseCode = "404", description = "Категория не найдена")
    @GetMapping("/{id}")
    public CategoriesModel getById(@PathVariable @Parameter(description = "ID категории") Long id) {
        return categoriesService.findById(id);
    }

    @Operation(summary = "Поиск по названию")
    @GetMapping("/search")
    public List<CategoriesModel> search(@RequestParam @Parameter(description = "Название категории") String name) {
        return categoriesService.findByName(name);
    }

    @Operation(summary = "Создать категорию")
    @ApiResponse(responseCode = "201", description = "Создано")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriesModel create(@Valid @RequestBody CategoriesModel category) {
        category.setId(null);   // клиент не должен подменять существующую запись
        return categoriesService.create(category);
    }

    @Operation(summary = "Обновить категорию")
    @ApiResponse(responseCode = "404", description = "Категория не найдена")
    @PutMapping("/{id}")
    public CategoriesModel update(@PathVariable Long id, @Valid @RequestBody CategoriesModel category) {
        category.setId(id);
        return categoriesService.update(category);
    }

    @Operation(summary = "Удалить категорию", description = "Нельзя удалить категорию, которая используется")
    @ApiResponse(responseCode = "204", description = "Удалено")
    @ApiResponse(responseCode = "409", description = "Категория используется мероприятиями")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        categoriesService.delete(id);
    }
}