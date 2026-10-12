package com.smarthelpdesk.apigateway.dto.response;

import java.util.UUID;

public record TicketOwnerResponse(
        UUID ticketId,
        UUID userId
) {
}
