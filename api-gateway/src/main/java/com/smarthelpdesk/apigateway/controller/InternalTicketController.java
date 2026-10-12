package com.smarthelpdesk.apigateway.controller;

import com.smarthelpdesk.apigateway.dto.response.TicketOwnerResponse;
import com.smarthelpdesk.apigateway.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/tickets")
@RequiredArgsConstructor
public class InternalTicketController {
    private final TicketService ticketService;

    @GetMapping("/{ticketId}/owner")
    public TicketOwnerResponse getOwner(@PathVariable("ticketId") UUID ticketId) {
        return new TicketOwnerResponse(ticketId, ticketService.findOwnerId(ticketId));
    }
}
