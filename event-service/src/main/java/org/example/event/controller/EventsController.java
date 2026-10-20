package org.example.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.event.dto.EventRequest;
import org.example.event.dto.EventResponse;
import org.example.event.service.EventService;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@Tag(name = "Мероприятия", description = "API для управления мероприятиями")
@SecurityRequirement(name = "BearerAuth")
public class EventsController {

    private static final String USER_ID = "X-User-Id";
    private static final String USER_ROLE = "X-User-Role";

    private final EventService eventService;

    public EventsController(EventService eventService) {
        this.eventService = eventService;
    }

    @Operation(summary = "Получить все мероприятия", description = "Список постранично, по дате начала")
    @GetMapping
    public PagedModel<EventResponse> getAll(
            @RequestParam(defaultValue = "0") @Parameter(description = "Номер страницы") int page,
            @RequestParam(defaultValue = "5") @Parameter(description = "Размер страницы") int size) {
        return new PagedModel<>(eventService.findPage(page, size));
    }

    @Operation(summary = "Получить мероприятие по ID")
    @ApiResponse(responseCode = "404", description = "Мероприятие не найдено")
    @GetMapping("/{id}")
    public EventResponse getById(@PathVariable @Parameter(description = "ID мероприятия") Long id) {
        return eventService.findById(id);
    }

    @Operation(summary = "Поиск по названию", description = "Совпадение по части названия, без учёта регистра")
    @GetMapping("/search")
    public List<EventResponse> search(@RequestParam @Parameter(description = "Часть названия") String title) {
        return eventService.searchByTitle(title);
    }

    @Operation(summary = "Мои мероприятия", description = "Мероприятия текущего организатора")
    @GetMapping("/my")
    public List<EventResponse> my(@Parameter(hidden = true) @RequestHeader(USER_ID) Long userId) {
        return eventService.findByOrganizer(userId);
    }

    @Operation(summary = "Создать мероприятие", description = "Организатором становится текущий пользователь")
    @ApiResponse(responseCode = "201", description = "Создано")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации или превышена вместимость")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(@Valid @RequestBody EventRequest request,
                                @Parameter(hidden = true) @RequestHeader(USER_ID) Long userId) {
        return eventService.create(request, userId);
    }

    @Operation(summary = "Обновить мероприятие", description = "Только организатор или ADMIN")
    @ApiResponse(responseCode = "403", description = "Нет прав на изменение")
    @ApiResponse(responseCode = "404", description = "Мероприятие не найдено")
    @PutMapping("/{id}")
    public EventResponse update(@PathVariable Long id,
                                @Valid @RequestBody EventRequest request,
                                @Parameter(hidden = true) @RequestHeader(USER_ID) Long userId,
                                @Parameter(hidden = true) @RequestHeader(USER_ROLE) String role) {
        return eventService.update(id, request, userId, role);
    }

    @Operation(summary = "Удалить мероприятие", description = "Только организатор или ADMIN")
    @ApiResponse(responseCode = "204", description = "Удалено")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
                       @Parameter(hidden = true) @RequestHeader(USER_ID) Long userId,
                       @Parameter(hidden = true) @RequestHeader(USER_ROLE) String role) {
        eventService.delete(id, userId, role);
    }
}