package com.smarthelpdesk.apigateway.dto.request;

import com.smarthelpdesk.apigateway.entity.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Данные для изменения статуса тикета.
 * Используется оператором для управления жизненным циклом тикета.
 */

public record UpdateTicketStatusRequest(
        @NotNull
        TicketStatus status
) {
}
