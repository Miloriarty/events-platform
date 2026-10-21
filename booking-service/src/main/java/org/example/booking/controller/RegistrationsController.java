package org.example.booking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.booking.dto.RegistrationRequest;
import org.example.booking.dto.RegistrationResponse;
import org.example.booking.service.RegistrationsService;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/registrations")
@Tag(name = "Регистрации", description = "Регистрации пользователей на мероприятия")
@SecurityRequirement(name = "BearerAuth")
public class RegistrationsController {

    private final RegistrationsService registrationsService;

    public RegistrationsController(RegistrationsService registrationsService) {
        this.registrationsService = registrationsService;
    }

    @Operation(summary = "Мои регистрации", description = "ADMIN видит регистрации всех пользователей")
    @GetMapping
    public PagedModel<RegistrationResponse> getAll(
            @RequestParam(defaultValue = "0") @Parameter(description = "Номер страницы") int page,
            @RequestParam(defaultValue = "5") @Parameter(description = "Размер страницы") int size,
            @Parameter(hidden = true) @RequestHeader(RequestHeaders.USER_ID) Long userId,
            @Parameter(hidden = true) @RequestHeader(RequestHeaders.USER_ROLE) String role) {
        return new PagedModel<>(registrationsService.findPage(page, size, userId, role));
    }

    @Operation(summary = "Получить регистрацию по ID")
    @ApiResponse(responseCode = "404", description = "Регистрация не найдена")
    @GetMapping("/{id}")
    public RegistrationResponse getById(
            @PathVariable @Parameter(description = "ID регистрации") Long id,
            @Parameter(hidden = true) @RequestHeader(RequestHeaders.USER_ID) Long userId,
            @Parameter(hidden = true) @RequestHeader(RequestHeaders.USER_ROLE) String role) {
        return registrationsService.findById(id, userId, role);
    }

    @Operation(summary = "Участники мероприятия", description = "Только организатор мероприятия или ADMIN")
    @ApiResponse(responseCode = "403", description = "Вы не организатор этого мероприятия")
    @GetMapping("/event/{eventId}")
    public List<RegistrationResponse> getByEvent(
            @PathVariable @Parameter(description = "ID мероприятия") Long eventId,
            @Parameter(hidden = true) @RequestHeader(RequestHeaders.USER_ID) Long userId,
            @Parameter(hidden = true) @RequestHeader(RequestHeaders.USER_ROLE) String role) {
        return registrationsService.findByEvent(eventId, userId, role);
    }

    @Operation(summary = "Зарегистрироваться на мероприятие",
            description = "По одной из позиций вашего заказа; одна регистрация занимает одно место")
    @ApiResponse(responseCode = "201", description = "Зарегистрирован")
    @ApiResponse(responseCode = "409", description = "Нет свободных мест или билеты по позиции исчерпаны")
    @ApiResponse(responseCode = "503", description = "Сервис мероприятий временно недоступен")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse create(
            @Valid @RequestBody RegistrationRequest request,
            @Parameter(hidden = true) @RequestHeader(RequestHeaders.USER_ID) Long userId) {
        return registrationsService.create(request, userId);
    }

    @Operation(summary = "Отменить регистрацию", description = "Место на мероприятии освобождается")
    @PostMapping("/{id}/cancel")
    public RegistrationResponse cancel(
            @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader(RequestHeaders.USER_ID) Long userId,
            @Parameter(hidden = true) @RequestHeader(RequestHeaders.USER_ROLE) String role) {
        return registrationsService.cancel(id, userId, role);
    }
}