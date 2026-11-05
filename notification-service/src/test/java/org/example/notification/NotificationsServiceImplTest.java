package org.example.notification;

import org.example.notification.dto.NotificationResponse;
import org.example.notification.messaging.BookingEvent;
import org.example.notification.model.EmailStatus;
import org.example.notification.model.NotificationType;
import org.example.notification.model.NotificationsModel;
import org.example.notification.repository.NotificationsRepository;
import org.example.notification.service.EmailSender;
import org.example.notification.service.NotificationStream;
import org.example.notification.service.NotificationTemplates;
import org.example.notification.service.NotificationsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NotificationsServiceImplTest {

    private final NotificationsRepository repository = mock(NotificationsRepository.class);
    private final NotificationStream stream = mock(NotificationStream.class);
    private final EmailSender email = mock(EmailSender.class);
    private final NotificationsServiceImpl service =
            new NotificationsServiceImpl(repository, new NotificationTemplates(), stream, email);

    private final BookingEvent event = new BookingEvent("m1", NotificationType.ORDER_CREATED, 7L, 12L,
            new BigDecimal("100"), null, null, null, LocalDateTime.now());

    @BeforeEach
    void saveReturnsEntity() {
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void deliversToBrowserAndEmail() {
        when(email.send(any())).thenReturn(EmailStatus.SENT);

        service.deliver(event);

        verify(stream).send(eq(7L), any(NotificationResponse.class));
        verify(email).send(any());
    }

    @Test
    void emailFailureKeepsNotification() {
        when(email.send(any())).thenReturn(EmailStatus.FAILED);

        service.deliver(event);

        verify(stream).send(eq(7L), any(NotificationResponse.class));
        verify(repository, times(2)).save(any(NotificationsModel.class));
    }

    @Test
    void repeatedMessageIsSkipped() {
        when(repository.existsByMessageId("m1")).thenReturn(true);

        service.deliver(event);

        verify(repository, never()).save(any());
        verifyNoInteractions(stream, email);
    }

    @Test
    void foreignNotificationIsNotFound() {
        when(repository.findByIdAndUserId(1L, 7L)).thenReturn(Optional.empty());

        var ex = assertThrows(ResponseStatusException.class,
                () -> service.markRead(1L, 7L));
        assertEquals(404, ex.getStatusCode().value());
    }
}
