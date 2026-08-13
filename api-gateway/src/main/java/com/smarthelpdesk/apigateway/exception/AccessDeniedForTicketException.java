package com.smarthelpdesk.apigateway.exception;

import org.apache.coyote.BadRequestException;

public class AccessDeniedForTicketException extends BadRequestException {
    public AccessDeniedForTicketException(java.util.UUID ticketId) {
        super("Доступ к тикету запрещён: " + ticketId);
    }
}
