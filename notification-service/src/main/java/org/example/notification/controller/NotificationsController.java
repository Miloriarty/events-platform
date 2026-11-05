package org.example.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.notification.dto.NotificationResponse;
import org.example.notification.dto.UnreadCountResponse;
import org.example.notification.service.NotificationStream;
import org.example.notification.service.NotificationsService;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Уведомления", description = "Личные уведомления текущего пользователя")
@SecurityRequirement(name = "BearerAuth")
public class NotificationsController {

    private final NotificationsService notifications;
    private final NotificationStream stream;

    public NotificationsController(NotificationsService notifications, NotificationStream stream) {
        this.notifications = notifications;
        this.stream = stream;
    }

    @Operation(summary = "Мои уведомления", description = "Сначала новые")
    @GetMapping
    public PagedModel<NotificationResponse> getAll(
            @RequestParam(defaultValue = "0") @Parameter(description = "Номер страницы") int page,
            @RequestParam(defaultValue = "10") @Parameter(description = "Размер страницы") int size,
            @Parameter(hidden = true) @RequestHeader(RequestHeaders.USER_ID) Long userId) {
        return new PagedModel<>(notifications.findPage(page, size, userId));
    }

    @Operation(summary = "Количество непрочитанных")
    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(
            @Parameter(hidden = true) @RequestHeader(RequestHeaders.USER_ID) Long userId) {
        return new UnreadCountResponse(notifications.countUnread(userId));
    }

    @Operation(summary = "Поток новых уведомлений (SSE)",
            description = "Событие `notification` с телом как в списке, раз в 25 секунд комментарий ping")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @Parameter(hidden = true) @RequestHeader(RequestHeaders.USER_ID) Long userId) {
        return stream.connect(userId);
    }

    @Operation(summary = "Отметить прочитанным")
    @ApiResponse(responseCode = "404", description = "Уведомление не найдено (или принадлежит другому пользователю)")
    @PostMapping("/{id}/read")
    public NotificationResponse markRead(
            @PathVariable @Parameter(description = "ID уведомления") Long id,
            @Parameter(hidden = true) @RequestHeader(RequestHeaders.USER_ID) Long userId) {
        return notifications.markRead(id, userId);
    }

    @Operation(summary = "Отметить все прочитанными")
    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(
            @Parameter(hidden = true) @RequestHeader(RequestHeaders.USER_ID) Long userId) {
        notifications.markAllRead(userId);
    }
}
