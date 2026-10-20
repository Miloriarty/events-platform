package org.example.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.event.model.LocationsModel;
import org.example.event.service.LocationsService;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@Tag(name = "Локации", description = "API для управления локациями")
@SecurityRequirement(name = "BearerAuth")
public class LocationsController {

    private final LocationsService locationsService;

    public LocationsController(LocationsService locationsService) {
        this.locationsService = locationsService;
    }

    @Operation(summary = "Получить все локации", description = "Список постранично")
    @GetMapping
    public PagedModel<LocationsModel> getAll(
            @RequestParam(defaultValue = "0") @Parameter(description = "Номер страницы") int page,
            @RequestParam(defaultValue = "5") @Parameter(description = "Размер страницы") int size) {
        return new PagedModel<>(locationsService.findPage(page, size));
    }

    @Operation(summary = "Получить локацию по ID")
    @ApiResponse(responseCode = "404", description = "Локация не найдена")
    @GetMapping("/{id}")
    public LocationsModel getById(@PathVariable @Parameter(description = "ID локации") Long id) {
        return locationsService.findById(id);
    }

    @Operation(summary = "Поиск локации по названию")
    @GetMapping("/search")
    public List<LocationsModel> search(@RequestParam @Parameter(description = "Название локации") String name) {
        return locationsService.findByName(name);
    }

    @Operation(summary = "Создать локацию")
    @ApiResponse(responseCode = "201", description = "Создано")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LocationsModel create(@Valid @RequestBody LocationsModel location) {
        location.setId(null);
        return locationsService.create(location);
    }

    @Operation(summary = "Обновить локацию")
    @ApiResponse(responseCode = "404", description = "Локация не найдена")
    @PutMapping("/{id}")
    public LocationsModel update(@PathVariable Long id, @Valid @RequestBody LocationsModel location) {
        location.setId(id);
        return locationsService.update(location);
    }

    @Operation(summary = "Удалить локацию", description = "Нельзя удалить локацию, которая используется")
    @ApiResponse(responseCode = "204", description = "Удалено")
    @ApiResponse(responseCode = "409", description = "Локация используется мероприятиями")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        locationsService.delete(id);
    }
}