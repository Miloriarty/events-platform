package org.example.reminder.service;

import org.example.reminder.client.BookingLookup;
import org.example.reminder.client.EventInfo;
import org.example.reminder.client.EventLookup;
import org.example.reminder.dto.ReminderRunResponse;
import org.example.reminder.messaging.ReminderEvent;
import org.example.reminder.messaging.ReminderPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReminderServiceImpl implements ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderServiceImpl.class);

    private final EventLookup events;
    private final BookingLookup booking;
    private final ReminderPublisher publisher;
    private final int daysBefore;

    public ReminderServiceImpl(EventLookup events, BookingLookup booking, ReminderPublisher publisher,
                               @Value("${reminder.days-before}") int daysBefore) {
        this.events = events;
        this.booking = booking;
        this.publisher = publisher;
        this.daysBefore = daysBefore;
    }

    @Override
    public ReminderRunResponse run() {
        LocalDate date = LocalDate.now().plusDays(daysBefore);
        List<EventInfo> starting = events.findStarting(date);
        int published = 0;
        int failed = 0;
        for (EventInfo event : starting) {
            try {
                for (Long userId : booking.findParticipants(event.id())) {
                    if (publisher.publish(ReminderEvent.of(event, userId))) {
                        published++;
                    } else {
                        failed++;
                    }
                }
            } catch (ResponseStatusException e) {
                // остальные мероприятия не должны страдать, это попадет в следующую проверку
                log.warn("Skipped reminders for event {}: {}", event.id(), e.getReason());
                failed++;
            }
        }
        log.info("Reminder run for {}: {} event(s), {} published, {} failed",
                date, starting.size(), published, failed);
        return new ReminderRunResponse(date, starting.size(), published, failed);
    }
}
