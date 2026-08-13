package com.smarthelpdesk.apigateway.service;


import com.smarthelpdesk.apigateway.entity.Ticket;
import com.smarthelpdesk.apigateway.entity.User;
import com.smarthelpdesk.apigateway.entity.enums.TicketStatus;
import com.smarthelpdesk.apigateway.exception.AccessDeniedForTicketException;
import com.smarthelpdesk.apigateway.exception.TicketNotFoundException;
import com.smarthelpdesk.apigateway.exception.UserNotFoundException;
import com.smarthelpdesk.apigateway.repository.TicketRepository;
import com.smarthelpdesk.apigateway.repository.TicketStatusHistoryRepository;
import com.smarthelpdesk.apigateway.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j

public class TicketService {
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketStatusHistoryRepository ticketStatusHistoryRepository;

    @Transactional
    public Ticket createTicket(String message, UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

        Ticket ticket = Ticket.builder()
                .user(user)
                .message(message)
                .status(TicketStatus.NEW)
                .build();

        Ticket saved = ticketRepository.save(ticket);

/*        recordStatusHistory(saved, null, TicketStatus.NEW, userId);

        TicketCreatedEvent event = TicketCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .ticketId(saved.getId())
                .userId(userId)
                .message(saved.getMessage())
                .customerTier(user.getCustomerTier().name())
                .createdAt(saved.getCreatedAt())
                .build();

        outboxPublisherService.saveEvent(
                "Ticket",
                saved.getId(),
                KafkaTopics.TICKET_CREATED_EVENT_TYPE,
                event
        );*/

        log.info("Ticket created: ticketId={}, userId={}", saved.getId(), userId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Ticket findById(UUID ticketId, UUID requesterId) throws AccessDeniedForTicketException {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        if (!ticket.getUser().getId().equals(requesterId)) {
            throw new AccessDeniedForTicketException(ticketId);
        }

        return ticket;
    }


    @Transactional(readOnly = true)
    public Page<Ticket> findAllForUser(UUID userId, Pageable pageable) {
        return ticketRepository.findByUserId(userId, pageable);
    }


}
