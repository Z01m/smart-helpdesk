package com.smarthelpdesk.notificationservice.dto;

import com.smarthelpdesk.notificationservice.entity.Notification;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(UUID id, UUID ticketId, NotificationType type,
        String content, boolean read, Instant createdAt) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getTicketId(), n.getType(),
                n.getContent(), n.isRead(), n.getCreatedAt());
    }
}
