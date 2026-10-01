package com.smarthelpdesk.apigateway.dto.response;

import java.util.UUID;

public record TicketOwnerResponse(
        UUID tickedId,
        UUID userId
) {
}
