package org.example.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.example.notification.model.NotificationType;
import org.example.notification.model.NotificationsModel;

import java.time.LocalDateTime;

@Schema(description = "Уведомление")
public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        @Schema(description = "Страница клиента, к которой относится уведомление", example = "/orders")
        String link,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(NotificationsModel n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getMessage(),
                n.getLink(), n.isRead(), n.getCreatedAt());
    }
}
