package org.example.notification.service;

import org.example.notification.dto.NotificationResponse;
import org.example.notification.messaging.BookingEvent;
import org.springframework.data.domain.Page;

public interface NotificationsService {

    Page<NotificationResponse> findPage(int page, int size, Long userId);

    long countUnread(Long userId);

    NotificationResponse markRead(Long id, Long userId);

    void markAllRead(Long userId);

    // сохраняет уведомление и доставляет его в браузер и на почту
    void deliver(BookingEvent event);
}
