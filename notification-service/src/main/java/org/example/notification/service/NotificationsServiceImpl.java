package org.example.notification.service;

import org.example.notification.dto.NotificationResponse;
import org.example.notification.messaging.BookingEvent;
import org.example.notification.model.NotificationsModel;
import org.example.notification.repository.NotificationsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationsServiceImpl implements NotificationsService {

    private static final Logger log = LoggerFactory.getLogger(NotificationsServiceImpl.class);

    private final NotificationsRepository notifications;
    private final NotificationTemplates templates;
    private final NotificationStream stream;
    private final EmailSender email;

    public NotificationsServiceImpl(NotificationsRepository notifications, NotificationTemplates templates,
                                    NotificationStream stream, EmailSender email) {
        this.notifications = notifications;
        this.templates = templates;
        this.stream = stream;
        this.email = email;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> findPage(int page, int size, Long userId) {
        Sort newestFirst = Sort.by(Sort.Direction.DESC, "createdAt", "id");
        return notifications.findByUserId(userId, PageRequest.of(page, size, newestFirst))
                .map(NotificationResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notifications.countByUserIdAndReadFalse(userId);
    }

    // чужое уведомление отвечает 404, как и чужой заказ
    @Override
    @Transactional
    public NotificationResponse markRead(Long id, Long userId) {
        NotificationsModel notification = notifications.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        notification.setRead(true);
        log.info("Notification id={} marked as read by user {}", id, userId);
        return NotificationResponse.from(notification);
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        int updated = notifications.markAllRead(userId);
        log.info("Marked {} notification(s) as read for user {}", updated, userId);
    }

    // Без общей транзакции: письмо нельзя откатить, а долгая отправка не должна держать соединение с БД.
    // Сбои браузера и почты сюда не доходят, поэтому повтор сообщения брокером не даст второго письма
    @Override
    public void deliver(BookingEvent event) {
        if (notifications.existsByMessageId(event.messageId())) {
            log.info("Skipped duplicate message {}", event.messageId());
            return;
        }
        NotificationsModel saved = notifications.save(templates.build(event));
        stream.send(saved.getUserId(), NotificationResponse.from(saved));
        saved.setEmailStatus(email.send(saved));
        notifications.save(saved);
        log.info("Delivered notification id={} ({}) to user {}, email {}",
                saved.getId(), saved.getType(), saved.getUserId(), saved.getEmailStatus());
    }
}
