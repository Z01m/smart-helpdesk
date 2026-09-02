package com.smarthelpdesk.apigateway.controller;

import com.smarthelpdesk.apigateway.dto.request.CreateTicketRequest;
import com.smarthelpdesk.apigateway.dto.request.UpdateTicketStatusRequest;
import com.smarthelpdesk.apigateway.dto.response.TicketResponse;
import com.smarthelpdesk.apigateway.entity.Ticket;
import com.smarthelpdesk.apigateway.entity.enums.TicketStatus;
import com.smarthelpdesk.apigateway.exception.AccessDeniedForTicketException;
import com.smarthelpdesk.apigateway.mapper.TicketMapper;
import com.smarthelpdesk.apigateway.security.CustomUserDetails;
import com.smarthelpdesk.apigateway.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static org.springframework.data.domain.Sort.Direction.DESC;

/**
 * Обрабатывает REST-запросы, связанные с тикетами.
 * Передаёт выполнение бизнес-логики в TicketService.
 */

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final TicketMapper ticketMapper;

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            Authentication authentication) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Ticket ticket = ticketService.createTicket(
                request.message(),
                userDetails.getId()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ticketMapper.toResponse(ticket));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TicketResponse> updateTicketStatus(
            @PathVariable("id") UUID ticketId,
            @Valid @RequestBody UpdateTicketStatusRequest request,
            Authentication authentication
    ) throws AccessDeniedForTicketException {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Ticket ticket = ticketService.updateStatus(
                        ticketId,
                request.status(),
                userDetails
                );

        return ResponseEntity.ok(
                ticketMapper.toResponse(ticket)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicketById(
            @PathVariable("id") UUID ticketId,
            Authentication authentication
    ) throws AccessDeniedForTicketException {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Ticket ticket = ticketService.findById(
                ticketId,
                userDetails
        );

        return ResponseEntity.ok(
                ticketMapper.toResponse(ticket)
        );
    }

    @GetMapping
    public ResponseEntity<Page<TicketResponse>> getMyTickets(
            Authentication authentication,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = DESC
            )
            Pageable pageable
    ) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Page<Ticket> tickets = ticketService.findAllForUser(
                userDetails.getId(),
                pageable
        );

        Page<TicketResponse> response =
                tickets.map(ticketMapper::toResponse);

        return ResponseEntity.ok(response);
    }
}
