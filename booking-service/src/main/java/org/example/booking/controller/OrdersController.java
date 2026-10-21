package org.example.booking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.booking.dto.OrderRequest;
import org.example.booking.dto.OrderResponse;
import org.example.booking.service.OrdersService;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Заказы", description = "Покупка билетов")
@SecurityRequirement(name = "BearerAuth")
public class OrdersController {

    private final OrdersService ordersService;

    public OrdersController(OrdersService ordersService) {
        this.ordersService = ordersService;
    }

    @Operation(summary = "Мои заказы", description = "ADMIN видит заказы всех пользователей")
    @GetMapping
    public PagedModel<OrderResponse> getAll(
            @RequestParam(defaultValue = "0") @Parameter(description = "Номер страницы") int page,
            @RequestParam(defaultValue = "5") @Parameter(description = "Размер страницы") int size,
            @Parameter(hidden = true) @RequestHeader(RequestHeaders.USER_ID) Long userId,
            @Parameter(hidden = true) @RequestHeader(RequestHeaders.USER_ROLE) String role) {
        return new PagedModel<>(ordersService.findPage(page, size, userId, role));
    }

    @Operation(summary = "Получить заказ по ID")
    @ApiResponse(responseCode = "404", description = "Заказ не найден (или принадлежит другому пользователю)")
    @GetMapping("/{id}")
    public OrderResponse getById(
            @PathVariable @Parameter(description = "ID заказа") Long id,
            @Parameter(hidden = true) @RequestHeader(RequestHeaders.USER_ID) Long userId,
            @Parameter(hidden = true) @RequestHeader(RequestHeaders.USER_ROLE) String role) {
        return ordersService.findById(id, userId, role);
    }

    @Operation(summary = "Создать заказ",
            description = "Цену и сумму считает сервер, остаток билетов уменьшается")
    @ApiResponse(responseCode = "201", description = "Заказ создан")
    @ApiResponse(responseCode = "404", description = "Тип билета не найден")
    @ApiResponse(responseCode = "409", description = "Недостаточно билетов")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(
            @Valid @RequestBody OrderRequest request,
            @Parameter(hidden = true) @RequestHeader(RequestHeaders.USER_ID) Long userId) {
        return ordersService.create(request, userId);
    }

    @Operation(summary = "Отменить заказ",
            description = "Билеты возвращаются в продажу, регистрации по заказу отменяются")
    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(
            @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader(RequestHeaders.USER_ID) Long userId,
            @Parameter(hidden = true) @RequestHeader(RequestHeaders.USER_ROLE) String role) {
        return ordersService.cancel(id, userId, role);
    }
}