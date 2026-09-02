package com.smarthelpdesk.apigateway.dto.response;

import org.springframework.data.domain.Page;

/**
 * Ответ со списком тикетов.
 * Используется для передачи списка тикетов вместе с информацией о пагинации.
 */

public record TicketListResponse(
        Page<TicketResponse> page
) {
}
