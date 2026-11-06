package org.example.reminder.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

// Проверка повторяется в течение дня, а не один раз: так она переживает сбой сервисов и простой,
// а участник, зарегистрировавшийся позже, тоже получит напоминание
@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final ReminderService reminders;

    public ReminderScheduler(ReminderService reminders) {
        this.reminders = reminders;
    }

    @Scheduled(cron = "${reminder.cron}")
    public void tick() {
        try {
            reminders.run();
        } catch (ResponseStatusException e) {
            log.warn("Reminder run skipped: {}", e.getReason());
        }
    }
}
