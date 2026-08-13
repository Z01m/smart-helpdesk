package com.smarthelpdesk.apigateway.dto.response;

import org.springframework.data.domain.Page;

public record TicketListResponse(
        Page<TicketResponse> page
) {
}
