package org.example.booking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.booking.model.TicketsModel;
import org.example.booking.service.TicketsService;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Билеты", description = "Типы билетов, их цены и остатки")
@SecurityRequirement(name = "BearerAuth")
public class TicketsController {

    private final TicketsService ticketsService;

    public TicketsController(TicketsService ticketsService) {
        this.ticketsService = ticketsService;
    }

    @Operation(summary = "Получить все билеты", description = "Список постранично")
    @GetMapping
    public PagedModel<TicketsModel> getAll(
            @RequestParam(defaultValue = "0") @Parameter(description = "Номер страницы") int page,
            @RequestParam(defaultValue = "5") @Parameter(description = "Размер страницы") int size) {
        return new PagedModel<>(ticketsService.findPage(page, size));
    }

    @Operation(summary = "Получить билет по ID")
    @ApiResponse(responseCode = "404", description = "Билет не найден")
    @GetMapping("/{id}")
    public TicketsModel getById(@PathVariable @Parameter(description = "ID билета") Long id) {
        return ticketsService.findById(id);
    }

    @Operation(summary = "Поиск билетов по типу")
    @GetMapping("/search")
    public List<TicketsModel> search(@RequestParam @Parameter(description = "Тип билета") String type) {
        return ticketsService.findByType(type);
    }

    @Operation(summary = "Создать билет")
    @ApiResponse(responseCode = "201", description = "Создано")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketsModel create(@Valid @RequestBody TicketsModel ticket) {
        ticket.setId(null);     // клиент не должен подменять существующую запись
        return ticketsService.create(ticket);
    }

    @Operation(summary = "Обновить билет")
    @ApiResponse(responseCode = "404", description = "Билет не найден")
    @PutMapping("/{id}")
    public TicketsModel update(@PathVariable Long id, @Valid @RequestBody TicketsModel ticket) {
        ticket.setId(id);
        return ticketsService.update(ticket);
    }

    @Operation(summary = "Удалить билет", description = "Нельзя удалить тип билета, на который есть заказы")
    @ApiResponse(responseCode = "204", description = "Удалено")
    @ApiResponse(responseCode = "409", description = "Билет используется в заказах")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        ticketsService.delete(id);
    }
}