package org.example.pass.service;

import org.example.pass.client.BookingLookup;
import org.example.pass.client.EventInfo;
import org.example.pass.client.EventLookup;
import org.example.pass.client.RegistrationInfo;
import org.example.pass.dto.CheckInResponse;
import org.example.pass.dto.PassResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.Base64;

@Service
public class PassServiceImpl implements PassService {

    private static final Logger log = LoggerFactory.getLogger(PassServiceImpl.class);

    private final BookingLookup booking;
    private final EventLookup events;
    private final PassTokens tokens;
    private final QrCodes qrCodes;
    private final PassPdf pdf;
    private final String frontendUrl;

    public PassServiceImpl(BookingLookup booking, EventLookup events, PassTokens tokens,
                           QrCodes qrCodes, PassPdf pdf,
                           @Value("${app.frontend-url}") String frontendUrl) {
        this.booking = booking;
        this.events = events;
        this.tokens = tokens;
        this.qrCodes = qrCodes;
        this.pdf = pdf;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public PassResponse getPass(Long registrationId, Long userId, String role) {
        Issued issued = issue(registrationId, userId, role);
        String qrCode = "data:image/png;base64," + Base64.getEncoder().encodeToString(issued.qrPng());
        return issued.pass(qrCode);
    }

    @Override
    public byte[] getPdf(Long registrationId, Long userId, String role) {
        Issued issued = issue(registrationId, userId, role);
        byte[] document = pdf.render(issued.pass(null), issued.qrPng());
        log.info("Generated ticket PDF for registration id={} by user {}", registrationId, userId);
        return document;
    }

    @Override
    public CheckInResponse checkIn(String token, Long userId, String role) {
        Long registrationId = tokens.verify(token).orElseThrow(() -> {
            log.warn("Rejected invalid or expired ticket token from user {}", userId);
            return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket is invalid or expired");
        });
        RegistrationInfo registration = booking.checkIn(registrationId, userId, role);
        log.info("Checked in registration id={} by user {} ({})", registrationId, userId, role);
        return CheckInResponse.from(registration);
    }

    private Issued issue(Long registrationId, Long userId, String role) {
        RegistrationInfo registration = booking.getRegistration(registrationId, userId, role);
        if (!registration.isConfirmed()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Registration is cancelled");
        }
        EventInfo event = events.getEvent(registration.eventId());
        if (event.endDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Event has already finished");
        }
        String token = tokens.issue(registration.id(), event.endDate());
        String checkInUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/check-in").queryParam("token", token).toUriString();
        return new Issued(registration, event, token, checkInUrl, qrCodes.png(checkInUrl));
    }

    private record Issued(RegistrationInfo registration, EventInfo event, String token,
                          String checkInUrl, byte[] qrPng) {

        PassResponse pass(String qrCode) {
            EventInfo.Location location = event.location();
            return new PassResponse(registration.id(), event.id(), event.title(), registration.ticketType(),
                    event.startDate(), event.endDate(), location.name(),
                    location.address() + ", " + location.city(), token, checkInUrl, qrCode);
        }
    }
}
