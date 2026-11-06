package org.example.reminder.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.reminder.dto.ReminderRunResponse;
import org.example.reminder.service.ReminderService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reminders")
@Tag(name = "Напоминания", description = "Напоминания участникам о скором мероприятии")
@SecurityRequirement(name = "BearerAuth")
public class RemindersController {

    private final ReminderService reminderService;

    public RemindersController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @Operation(summary = "Запустить проверку сейчас",
            description = "Только ADMIN. Обычно проверка идёт по расписанию; повторный запуск не даёт вторых писем")
    @ApiResponse(responseCode = "503", description = "Сервис мероприятий временно недоступен")
    @PostMapping("/run")
    public ReminderRunResponse run() {
        return reminderService.run();
    }
}
