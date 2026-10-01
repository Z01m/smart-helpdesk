package com.smarthelpdesk.notificationservice.dto.gateway;

import java.util.UUID;

public record TicketOwnerResponse(
        UUID ticketId,
        UUID userId
) {
}