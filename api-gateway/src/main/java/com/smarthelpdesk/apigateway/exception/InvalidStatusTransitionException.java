package com.smarthelpdesk.apigateway.exception;

import com.smarthelpdesk.apigateway.entity.enums.TicketStatus;

/**
 * Ошибка недопустимого перехода статуса тикета.
 * Возникает при нарушении правил State Machine.
 */

public class InvalidStatusTransitionException extends ValidationException {
    public InvalidStatusTransitionException(TicketStatus from, TicketStatus to) {
        super(String.format("Недопустимый переход статуса: %s -> %s", from, to));
    }
}
