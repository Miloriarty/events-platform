package org.example.notification.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_notifications_user", columnList = "user_id, created_at"))
public class NotificationsModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // id исходного сообщения: брокер может доставить его повторно
    @Column(name = "message_id", nullable = false, unique = true)
    private String messageId;

    // id пользователя из user-service
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false)
    private String link;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_status", nullable = false)
    private EmailStatus emailStatus = EmailStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public NotificationsModel() {}

    public NotificationsModel(String messageId, Long userId, NotificationType type,
                              String title, String message, String link, LocalDateTime createdAt) {
        this.messageId = messageId;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.link = link;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getMessageId() { return messageId; }
    public Long getUserId() { return userId; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getLink() { return link; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public EmailStatus getEmailStatus() { return emailStatus; }
    public void setEmailStatus(EmailStatus emailStatus) { this.emailStatus = emailStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
