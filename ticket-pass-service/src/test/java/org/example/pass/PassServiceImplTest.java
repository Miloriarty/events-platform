package org.example.pass;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.example.pass.client.BookingLookup;
import org.example.pass.client.EventInfo;
import org.example.pass.client.EventLookup;
import org.example.pass.client.RegistrationInfo;
import org.example.pass.dto.CheckInResponse;
import org.example.pass.dto.PassResponse;
import org.example.pass.service.PassPdf;
import org.example.pass.service.PassServiceImpl;
import org.example.pass.service.PassTokens;
import org.example.pass.service.QrCodes;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PassServiceImplTest {

    private final BookingLookup booking = mock(BookingLookup.class);
    private final EventLookup events = mock(EventLookup.class);
    private final PassTokens tokens = new PassTokens("test-secret-test-secret-test-secret-test-secret");
    private final PassServiceImpl service = new PassServiceImpl(booking, events, tokens, new QrCodes(), new PassPdf(),
            "http://localhost:4200");

    private final LocalDate tomorrow = LocalDate.now().plusDays(1);

    private RegistrationInfo registration(String status) {
        return new RegistrationInfo(5L, 7L, 42L, "Java Meetup", "VIP", status, null);
    }

    private EventInfo event(LocalDate end) {
        return new EventInfo(42L, "Java Meetup", end.minusDays(1), end,
                new EventInfo.Location("Экспоцентр", "Ленина, 1", "Москва"));
    }

    @Test
    void passContainsVerifiableTokenAndQr() {
        when(booking.getRegistration(5L, 7L, "USER")).thenReturn(registration("CONFIRMED"));
        when(events.getEvent(42L)).thenReturn(event(tomorrow));

        PassResponse pass = service.getPass(5L, 7L, "USER");

        assertEquals(5L, tokens.verify(pass.token()).orElseThrow());
        assertEquals("http://localhost:4200/check-in?token=" + pass.token(), pass.checkInUrl());
        assertTrue(pass.qrCode().startsWith("data:image/png;base64,"));
        assertEquals("Ленина, 1, Москва", pass.locationAddress());
    }

    @Test
    void cancelledRegistrationHasNoPass() {
        when(booking.getRegistration(5L, 7L, "USER")).thenReturn(registration("CANCELLED"));

        var e = assertThrows(ResponseStatusException.class, () -> service.getPass(5L, 7L, "USER"));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
    }

    @Test
    void finishedEventHasNoPass() {
        when(booking.getRegistration(5L, 7L, "USER")).thenReturn(registration("CONFIRMED"));
        when(events.getEvent(42L)).thenReturn(event(LocalDate.now().minusDays(1)));

        var e = assertThrows(ResponseStatusException.class, () -> service.getPass(5L, 7L, "USER"));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
    }

    @Test
    void pdfShowsCyrillicText() throws IOException {
        when(booking.getRegistration(5L, 7L, "USER")).thenReturn(registration("CONFIRMED"));
        when(events.getEvent(42L)).thenReturn(event(tomorrow));

        byte[] pdf = service.getPdf(5L, 7L, "USER");

        try (PdfReader reader = new PdfReader(pdf)) {
            String text = new PdfTextExtractor(reader).getTextFromPage(1);
            assertTrue(text.contains("Экспоцентр"), text);
            assertTrue(text.contains("Java Meetup"), text);
        }
    }

    @Test
    void checkInPassesUserToBooking() {
        String token = tokens.issue(5L, tomorrow);
        RegistrationInfo checkedIn = new RegistrationInfo(5L, 7L, 42L, "Java Meetup", "VIP", "CONFIRMED",
                LocalDateTime.now());
        when(booking.checkIn(5L, 2L, "MANAGER")).thenReturn(checkedIn);

        CheckInResponse result = service.checkIn(token, 2L, "MANAGER");

        assertEquals(5L, result.registrationId());
        assertEquals(checkedIn.checkedInAt(), result.checkedInAt());
    }

    @Test
    void invalidTokenDoesNotReachBooking() {
        var e = assertThrows(ResponseStatusException.class, () -> service.checkIn("garbage", 2L, "MANAGER"));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        verify(booking, never()).checkIn(any(), any(), any());
    }
}
