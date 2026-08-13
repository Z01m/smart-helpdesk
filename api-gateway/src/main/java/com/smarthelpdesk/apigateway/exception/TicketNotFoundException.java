package com.smarthelpdesk.apigateway.exception;

import java.util.UUID;

public class TicketNotFoundException extends NotFoundException{
    public TicketNotFoundException(UUID ticketId){
        super("ticket not founded" + ticketId);
    }
}
