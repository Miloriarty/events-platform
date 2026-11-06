package org.example.reminder.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Итог проверки")
public record ReminderRunResponse(
        @Schema(description = "Дата начала проверенных мероприятий") LocalDate date,
        @Schema(description = "Сколько мероприятий начинается в этот день") int events,
        @Schema(description = "Сколько сообщений отправлено; повторные отбрасывает notification-service") int published,
        @Schema(description = "Сколько мероприятий или сообщений обработать не удалось") int failed
) {}
