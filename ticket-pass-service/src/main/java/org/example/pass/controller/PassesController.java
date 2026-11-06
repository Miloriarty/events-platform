package org.example.pass.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.pass.dto.CheckInRequest;
import org.example.pass.dto.CheckInResponse;
import org.example.pass.dto.PassResponse;
import org.example.pass.service.PassService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/passes")
@Tag(name = "Билеты", description = "Электронные билеты с QR-кодом и проверка на входе")
@SecurityRequirement(name = "BearerAuth")
public class PassesController {

    private final PassService passService;

    public PassesController(PassService passService) {
        this.passService = passService;
    }

    @Operation(summary = "Билет с QR-кодом", description = "По своей подтверждённой регистрации; ADMIN — по любой")
    @ApiResponse(responseCode = "404", description = "Регистрация не найдена")
    @ApiResponse(responseCode = "409", description = "Регистрация отменена или мероприятие завершилось")
    @ApiResponse(responseCode = "503", description = "Сервис бронирования или мероприятий недоступен")
    @GetMapping("/{registrationId}")
    public PassResponse get(
            @PathVariable @Parameter(description = "ID регистрации") Long registrationId,
            @Parameter(hidden = true) @RequestHeader(RequestHeaders.USER_ID) Long userId,
            @Parameter(hidden = true) @RequestHeader(RequestHeaders.USER_ROLE) String role) {
        return passService.getPass(registrationId, userId, role);
    }

    @Operation(summary = "Билет в PDF")
    @ApiResponse(responseCode = "404", description = "Регистрация не найдена")
    @ApiResponse(responseCode = "409", description = "Регистрация отменена или мероприятие завершилось")
    @GetMapping(value = "/{registrationId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(
            @PathVariable @Parameter(description = "ID регистрации") Long registrationId,
            @Parameter(hidden = true) @RequestHeader(RequestHeaders.USER_ID) Long userId,
            @Parameter(hidden = true) @RequestHeader(RequestHeaders.USER_ROLE) String role) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("ticket-" + registrationId + ".pdf").build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(passService.getPdf(registrationId, userId, role));
    }

    @Operation(summary = "Проверить билет на входе",
            description = "Только организатор мероприятия или ADMIN. Каждый билет проходит один раз")
    @ApiResponse(responseCode = "400", description = "Токен неверный или срок билета истёк")
    @ApiResponse(responseCode = "403", description = "Вы не организатор этого мероприятия")
    @ApiResponse(responseCode = "409", description = "Билет уже использован или регистрация отменена")
    @PostMapping("/check-in")
    public CheckInResponse checkIn(
            @Valid @RequestBody CheckInRequest request,
            @Parameter(hidden = true) @RequestHeader(RequestHeaders.USER_ID) Long userId,
            @Parameter(hidden = true) @RequestHeader(RequestHeaders.USER_ROLE) String role) {
        return passService.checkIn(request.token(), userId, role);
    }
}
