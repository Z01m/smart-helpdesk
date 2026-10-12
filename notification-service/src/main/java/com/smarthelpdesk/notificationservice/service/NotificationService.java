package com.smarthelpdesk.notificationservice.service;

import com.smarthelpdesk.notificationservice.client.GatewayClient;
import com.smarthelpdesk.notificationservice.dto.*;
import com.smarthelpdesk.notificationservice.entity.Notification;
import com.smarthelpdesk.notificationservice.repository.*;
import kafka.event.TicketProcessedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final ProcessedEventRepository processedEvents;
    private final NotificationRepository notifications;
    private final GatewayClient gateway;

    @Transactional
    public void process(UUID eventId, TicketProcessedEvent event) {
        if (eventId == null || event == null || event.ticketId() == null)
            throw new IllegalArgumentException("eventId and ticketId are required");
        if (processedEvents.claim(eventId) == 0) return;
        var owner = gateway.getTicketOwner(event.ticketId());
        String content = event.requiresHumanReview()
                ? "Ваше обращение обработано ИИ и передано оператору."
                : "По вашему обращению подготовлен ответ.";
        notifications.save(Notification.builder().userId(owner.userId()).ticketId(event.ticketId())
                .type(NotificationType.TICKET_PROCESSED).content(content).read(false).build());
    }

    @Transactional
    public void processStatusChange(UUID eventId, kafka.event.TicketStatusChangedEvent event) {
        if (eventId == null || event == null || event.ticketId() == null || event.toStatus() == null || event.toStatus().isBlank())
            throw new IllegalArgumentException("eventId, ticketId and toStatus are required");
        if (processedEvents.claim(eventId) == 0) return;
        var owner = gateway.getTicketOwner(event.ticketId());
        notifications.save(Notification.builder().userId(owner.userId()).ticketId(event.ticketId())
                .type(NotificationType.TICKET_STATUS_CHANGED)
                .content("Статус вашего обращения изменён: " + event.toStatus()).read(false).build());
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(UUID userId, boolean unreadOnly, int page, int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page >= 0 and size between 1 and 100 required");
        var pageable = PageRequest.of(page, size);
        var result = unreadOnly ? notifications.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId, pageable)
                : notifications.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return result.map(NotificationResponse::from);
    }

    @Transactional
    public void acknowledge(UUID userId, UUID id) {
        if (notifications.acknowledge(id, userId) == 0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found");
    }

    @Transactional
    public NotificationResponse markRead(UUID userId, UUID id) {
        var notification = notifications.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        notification.setRead(true);
        return NotificationResponse.from(notification);
    }
}
