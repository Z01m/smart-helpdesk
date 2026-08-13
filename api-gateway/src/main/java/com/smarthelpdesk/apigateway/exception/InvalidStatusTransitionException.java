package com.smarthelpdesk.apigateway.exception;

import com.smarthelpdesk.apigateway.entity.enums.TicketStatus;

public class InvalidStatusTransitionException extends ValidationException {
    public InvalidStatusTransitionException(TicketStatus from, TicketStatus to) {
        super(String.format("Недопустимый переход статуса: %s -> %s", from, to));
    }
}
