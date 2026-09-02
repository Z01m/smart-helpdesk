package com.smarthelpdesk.apigateway.exception;

import org.apache.coyote.BadRequestException;

/**
 * Ошибка доступа к тикету.
 * Возникает, когда пользователь пытается работать с чужим тикетом.
 */

public class AccessDeniedForTicketException extends BadRequestException {
    public AccessDeniedForTicketException(java.util.UUID ticketId) {
        super("Доступ к тикету запрещён: " + ticketId);
    }
}
