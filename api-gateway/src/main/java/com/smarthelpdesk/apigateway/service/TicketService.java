package com.smarthelpdesk.apigateway.service;


import com.smarthelpdesk.apigateway.entity.Ticket;
import com.smarthelpdesk.apigateway.entity.TicketStatusHistory;
import com.smarthelpdesk.apigateway.entity.User;
import com.smarthelpdesk.apigateway.entity.enums.Role;
import com.smarthelpdesk.apigateway.entity.enums.TicketStatus;
import com.smarthelpdesk.apigateway.exception.AccessDeniedForTicketException;
import com.smarthelpdesk.apigateway.exception.InvalidStatusTransitionException;
import com.smarthelpdesk.apigateway.exception.TicketNotFoundException;
import com.smarthelpdesk.apigateway.exception.UserNotFoundException;
import com.smarthelpdesk.apigateway.repository.TicketRepository;
import com.smarthelpdesk.apigateway.repository.TicketStatusHistoryRepository;
import com.smarthelpdesk.apigateway.repository.UserRepository;
import com.smarthelpdesk.apigateway.security.CustomUserDetails;
import kafka.KafkaTopics;
import kafka.event.TicketCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
/**
 * Содержит основную бизнес-логику работы с тикетами.
 * Управляет созданием, поиском, статусами и назначением операторов.
 */
public class TicketService {
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketStatusHistoryRepository ticketStatusHistoryRepository;
    private final OutboxPublisherService outboxPublisherService;

    @Transactional
    public Ticket createTicket(String message, UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

        Ticket ticket = Ticket.builder()
                .user(user)
                .message(message)
                .status(TicketStatus.NEW)
                .build();

        Ticket saved = ticketRepository.save(ticket);

        TicketCreatedEvent event =  TicketCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .ticketId(saved.getId())
                .userId(userId)
                .message(saved.getMessage())
                .customerTier(user.getCustomerTier().name())
                .createdAt(Instant.now())
                .build();

        outboxPublisherService.saveEvent(
                "Ticket",
                saved.getId(),
                KafkaTopics.TICKET_CREATED.name(),
                event
        );

        log.info("Ticket created: ticketId={}, userId={}", saved.getId(), userId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Ticket findById(
            UUID ticketId,
            CustomUserDetails currentUser
    ) throws AccessDeniedForTicketException {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        if (!isOperatorOrAdmin(currentUser)
                && !ticket.getUser().getId().equals(currentUser.getId())) {

            throw new AccessDeniedForTicketException(ticketId);
        }

        return ticket;
    }


    @Transactional(readOnly = true)
    public Page<Ticket> findAllForUser(UUID userId, Pageable pageable) {
        return ticketRepository.findByUserId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Ticket> findAllForOperators(
            CustomUserDetails currentUser,
            Pageable pageable
    ) {

        if (!isOperatorOrAdmin(currentUser)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only operators and admins can view all tickets"
            );
        }

        return ticketRepository.findAll(pageable);
    }

    @Transactional
    public Ticket assignOperator(UUID ticketId, UUID operatorId){
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new TicketNotFoundException(ticketId));
        User operator = userRepository.findById(operatorId).orElseThrow(() -> new UserNotFoundException(operatorId));
        ticket.setOperator(operator);
        return ticketRepository.save(ticket);
    }

    @Transactional
    public Ticket updateStatus(UUID ticketId, TicketStatus newStatus, CustomUserDetails currentUser ) throws AccessDeniedForTicketException {

        if (!isOperatorOrAdmin(currentUser)){
            throw new AccessDeniedForTicketException(ticketId);
        }

        Ticket ticket = findById(ticketId, currentUser);

        TicketStatus oldStatus = ticket.getStatus();

        if (oldStatus == newStatus) {
            return ticket;
        }

        validateStatusTransition(oldStatus, newStatus);

        TicketStatusHistory history = TicketStatusHistory.builder()
                .ticket(ticket)
                .fromStatus(oldStatus)
                .toStatus(newStatus)
                .changedBy(currentUser.getId())
                .build();

        ticketStatusHistoryRepository.save(history);
        ticket.setStatus(newStatus);
        return ticketRepository.save(ticket);
    }

    private void validateStatusTransition(
            TicketStatus oldStatus,
            TicketStatus newStatus
    ) {
        if (oldStatus == null || newStatus == null) {
            throw new InvalidStatusTransitionException(
                    oldStatus,
                    newStatus
            );
        }

        if (oldStatus == newStatus) {
            return;
        }

        boolean allowed = switch (oldStatus) {

            case NEW ->
                    newStatus == TicketStatus.PROCESSING;

            case PROCESSING ->
                    newStatus == TicketStatus.CLASSIFIED
                            || newStatus == TicketStatus.FAILED;

            case CLASSIFIED ->
                    newStatus == TicketStatus.ANSWED
                            || newStatus == TicketStatus.WAITIND_OPERATOR;

            case ANSWED ->
                    newStatus == TicketStatus.CLOSED
                            || newStatus == TicketStatus.WAITIND_OPERATOR;

            case WAITIND_OPERATOR ->
                    newStatus == TicketStatus.ANSWED
                            || newStatus == TicketStatus.CLOSED;

            case CLOSED ->
                    newStatus == TicketStatus.REOPENED;

            case REOPENED ->
                    newStatus == TicketStatus.PROCESSING;

            case FAILED ->
                    newStatus == TicketStatus.WAITIND_OPERATOR;
        };

        if (!allowed) {
            throw new InvalidStatusTransitionException(
                    oldStatus,
                    newStatus
            );
        }
    }

    private boolean isOperatorOrAdmin(CustomUserDetails currentUser) {
        return currentUser.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_OPERATOR")
                                || authority.getAuthority().equals("ROLE_ADMIN")
                );
    }

    @Transactional
    public Ticket reopen(UUID ticketId, CustomUserDetails currentUser) throws AccessDeniedForTicketException {
        return updateStatus(ticketId, TicketStatus.REOPENED, currentUser);
    }

}
