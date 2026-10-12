package com.smarthelpdesk.notificationservice;

import com.smarthelpdesk.notificationservice.client.GatewayClient;
import com.smarthelpdesk.notificationservice.dto.gateway.TicketOwnerResponse;
import com.smarthelpdesk.notificationservice.entity.Notification;
import com.smarthelpdesk.notificationservice.kafka.TicketProcessedConsumer;
import com.smarthelpdesk.notificationservice.repository.*;
import com.smarthelpdesk.notificationservice.service.NotificationService;
import kafka.event.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationUnitTest {
    private final ProcessedEventRepository processed = mock(ProcessedEventRepository.class);
    private final NotificationRepository notifications = mock(NotificationRepository.class);
    private final GatewayClient gateway = mock(GatewayClient.class);
    private final NotificationService service = new NotificationService(processed, notifications, gateway);
    @Test void duplicateDoesNotCallGatewayOrCreateNotification() {
        service.process(UUID.randomUUID(), TicketProcessedEvent.builder().ticketId(UUID.randomUUID()).build());
        verifyNoInteractions(gateway, notifications);
    }
    @Test void createsNotificationForOwnerReturnedByGateway() {
        UUID event = UUID.randomUUID(), ticket = UUID.randomUUID(), user = UUID.randomUUID();
        when(processed.claim(event)).thenReturn(1);
        when(gateway.getTicketOwner(ticket)).thenReturn(new TicketOwnerResponse(ticket, user));
        service.process(event, TicketProcessedEvent.builder().ticketId(ticket).requiresHumanReview(true).build());
        var captor = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(user);
        assertThat(captor.getValue().getContent()).contains("оператору");
    }
    @Test void cannotMarkAnotherUsersNotificationRead() {
        UUID user = UUID.randomUUID(), id = UUID.randomUUID();
        when(notifications.findByIdAndUserId(id, user)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.markRead(user, id)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void consumerDoesNotAcknowledgeFailedPersistence() {
        var failedService = mock(NotificationService.class);
        doThrow(new IllegalStateException("db unavailable")).when(failedService).process(any(), any());
        var acknowledgment = mock(Acknowledgment.class);
        var mapper = JsonMapper.builder().build();
        var envelope = EventEnvelope.<TicketProcessedEvent>builder().eventId(UUID.randomUUID())
                .payload(TicketProcessedEvent.builder().ticketId(UUID.randomUUID()).build()).build();
        var consumer = new TicketProcessedConsumer(mapper, failedService);
        assertThatThrownBy(() -> consumer.consume(mapper.writeValueAsString(envelope), acknowledgment))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(acknowledgment);
    }
    @Test void invalidEnvelopeCannotBeAcknowledged() {
        var acknowledgment = mock(Acknowledgment.class);
        assertThatThrownBy(() -> new TicketProcessedConsumer(JsonMapper.builder().build(), service)
                .consume("null", acknowledgment)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(acknowledgment, processed);
    }
}
