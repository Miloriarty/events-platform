package org.example.notification.service;

import org.example.notification.client.UserInfo;
import org.example.notification.client.UserLookup;
import org.example.notification.model.EmailStatus;
import org.example.notification.model.NotificationsModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final JavaMailSender mail;
    private final UserLookup users;
    private final String from;
    private final String frontendUrl;

    public EmailSender(JavaMailSender mail, UserLookup users,
                       @Value("${app.mail.from}") String from,
                       @Value("${app.frontend-url}") String frontendUrl) {
        this.mail = mail;
        this.users = users;
        this.from = from;
        this.frontendUrl = frontendUrl;
    }

    // Письмо отправляется одним заходом: при сбое уведомление остается в истории и в браузере
    public EmailStatus send(NotificationsModel notification) {
        Optional<UserInfo> user = users.find(notification.getUserId());
        if (user.isEmpty()) {
            log.warn("Email for notification id={} not sent: no address for user {}",
                    notification.getId(), notification.getUserId());
            return EmailStatus.FAILED;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(user.get().email());
        message.setSubject(notification.getTitle());
        message.setText("""
                Здравствуйте, %s!

                %s

                Подробнее: %s%s

                Events Platform
                """.formatted(user.get().fullName(), notification.getMessage(), frontendUrl, notification.getLink()));
        try {
            mail.send(message);
            return EmailStatus.SENT;
        } catch (MailException e) {
            log.warn("Email for notification id={} not sent: {}", notification.getId(), e.getMessage());
            return EmailStatus.FAILED;
        }
    }
}
