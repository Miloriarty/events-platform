package org.example.reminder.service;

import org.example.reminder.dto.ReminderRunResponse;

public interface ReminderService {

    // отправляет участникам мероприятий, до начала которых осталось reminder.days-before дней
    ReminderRunResponse run();
}
