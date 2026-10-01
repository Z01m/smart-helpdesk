package com.smarthelpdesk.apigateway.controller;

import com.smarthelpdesk.apigateway.dto.response.TicketOwnerResponse;
import com.smarthelpdesk.apigateway.entity.Ticket;
import com.smarthelpdesk.apigateway.exception.AccessDeniedForTicketException;
import com.smarthelpdesk.apigateway.security.CustomUserDetails;
import com.smarthelpdesk.apigateway.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class InternalTicketController {

    TicketService ticketService;

    @GetMapping
    @PatchMapping("/{ticketId}/owner")
    public ResponseEntity<TicketOwnerResponse> getOwner(
            @PathVariable("ticketId") UUID ticketId, Authentication authentication) throws AccessDeniedForTicketException {

        UUID userId = ticketService.findOwnerId(ticketId);

        TicketOwnerResponse response =
                new TicketOwnerResponse(ticketId, userId);

        return ResponseEntity.ok(response);

    }
    
}
