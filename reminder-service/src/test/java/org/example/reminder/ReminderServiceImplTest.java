package org.example.reminder;

import org.example.reminder.client.BookingLookup;
import org.example.reminder.client.EventInfo;
import org.example.reminder.client.EventLookup;
import org.example.reminder.dto.ReminderRunResponse;
import org.example.reminder.messaging.ReminderEvent;
import org.example.reminder.messaging.ReminderPublisher;
import org.example.reminder.service.ReminderServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReminderServiceImplTest {

    private final EventLookup events = mock(EventLookup.class);
    private final BookingLookup booking = mock(BookingLookup.class);
    private final ReminderPublisher publisher = mock(ReminderPublisher.class);
    private final ReminderServiceImpl service = new ReminderServiceImpl(events, booking, publisher, 1);

    private final LocalDate tomorrow = LocalDate.now().plusDays(1);
    private final EventInfo meetup = new EventInfo(42L, "Java Meetup", tomorrow);

    @Test
    void remindsEveryParticipantOfTomorrowsEvents() {
        when(events.findStarting(tomorrow)).thenReturn(List.of(meetup));
        when(booking.findParticipants(42L)).thenReturn(List.of(7L, 8L));
        when(publisher.publish(any())).thenReturn(true);

        ReminderRunResponse result = service.run();

        assertEquals(new ReminderRunResponse(tomorrow, 1, 2, 0), result);
        ArgumentCaptor<ReminderEvent> sent = ArgumentCaptor.forClass(ReminderEvent.class);
        verify(publisher, times(2)).publish(sent.capture());
        assertEquals(7L, sent.getAllValues().get(0).userId());
        assertEquals("EVENT_REMINDER", sent.getAllValues().get(0).type());
        assertEquals(tomorrow, sent.getAllValues().get(0).eventDate());
    }

    @Test
    void messageIdIsStableAcrossRunsSoDuplicatesAreDropped() {
        assertEquals(ReminderEvent.of(meetup, 7L).messageId(), ReminderEvent.of(meetup, 7L).messageId());
    }

    @Test
    void movedEventGetsNewReminder() {
        EventInfo moved = new EventInfo(42L, "Java Meetup", tomorrow.plusDays(7));
        assertNotEquals(ReminderEvent.of(meetup, 7L).messageId(), ReminderEvent.of(moved, 7L).messageId());
    }

    @Test
    void failureOfOneEventDoesNotStopOthers() {
        EventInfo other = new EventInfo(43L, "Kotlin Day", tomorrow);
        when(events.findStarting(tomorrow)).thenReturn(List.of(meetup, other));
        when(booking.findParticipants(42L)).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));
        when(booking.findParticipants(43L)).thenReturn(List.of(7L));
        when(publisher.publish(any())).thenReturn(true);

        assertEquals(new ReminderRunResponse(tomorrow, 2, 1, 1), service.run());
    }

    @Test
    void brokerFailureIsCounted() {
        when(events.findStarting(tomorrow)).thenReturn(List.of(meetup));
        when(booking.findParticipants(42L)).thenReturn(List.of(7L));
        when(publisher.publish(any())).thenReturn(false);

        assertEquals(new ReminderRunResponse(tomorrow, 1, 0, 1), service.run());
    }
}
