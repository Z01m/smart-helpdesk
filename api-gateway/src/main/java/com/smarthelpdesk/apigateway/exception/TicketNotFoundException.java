package com.smarthelpdesk.apigateway.exception;

import java.util.UUID;
/**
 * Ошибка, возникающая при отсутствии тикета.
 * Обычно приводит к HTTP 404 Not Found.
 */
public class TicketNotFoundException extends NotFoundException{
    public TicketNotFoundException(UUID ticketId){
        super("ticket not founded" + ticketId);
    }
}
