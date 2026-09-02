package com.smarthelpdesk.apigateway.servise;

import com.smarthelpdesk.apigateway.entity.Ticket;
import com.smarthelpdesk.apigateway.entity.User;
import com.smarthelpdesk.apigateway.entity.enums.CustomerTier;
import com.smarthelpdesk.apigateway.entity.enums.Role;
import com.smarthelpdesk.apigateway.entity.enums.TicketStatus;
import com.smarthelpdesk.apigateway.exception.AccessDeniedForTicketException;
import com.smarthelpdesk.apigateway.exception.TicketNotFoundException;
import com.smarthelpdesk.apigateway.exception.UserNotFoundException;
import com.smarthelpdesk.apigateway.repository.TicketRepository;
import com.smarthelpdesk.apigateway.repository.TicketStatusHistoryRepository;
import com.smarthelpdesk.apigateway.repository.UserRepository;
import com.smarthelpdesk.apigateway.security.CustomUserDetails;
import com.smarthelpdesk.apigateway.service.OutboxPublisherService;
import com.smarthelpdesk.apigateway.service.TicketService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TicketStatusHistoryRepository ticketStatusHistoryRepository;

    @Mock
    private OutboxPublisherService outboxPublisherService;

    @InjectMocks
    private TicketService ticketService;

    private User user;
    private UUID userId;
    private CustomUserDetails customUserDetails;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .email("test@example.com")
                .role(Role.CUSTOMER)
                .customerTier(CustomerTier.STANDARD)
                .build();

        customUserDetails = new CustomUserDetails(user);
    }

    @Test
    void createTicket_shouldSaveTicketWithNewStatus() {

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(ticketRepository.save(any(Ticket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Ticket result = ticketService.createTicket(
                "Проблема с оплатой",
                userId
        );

        assertThat(result.getStatus())
                .isEqualTo(TicketStatus.NEW);

        assertThat(result.getMessage())
                .isEqualTo("Проблема с оплатой");

        assertThat(result.getUser())
                .isEqualTo(user);
    }

    @Test
    void createTicket_shouldThrowWhenUserNotFound() {

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> ticketService.createTicket(
                        "текст",
                        userId
                )
        )
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void findById_shouldReturnTicketWhenOwnerMatches()
            throws AccessDeniedForTicketException {

        UUID ticketId = UUID.randomUUID();

        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .user(user)
                .status(TicketStatus.NEW)
                .build();

        when(ticketRepository.findById(ticketId))
                .thenReturn(Optional.of(ticket));

        Ticket result = ticketService.findById(
                ticketId,
                customUserDetails
        );

        assertThat(result.getId())
                .isEqualTo(ticketId);
    }

    @Test
    void findById_shouldThrowWhenNotOwner() {

        UUID ticketId = UUID.randomUUID();

        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .user(user)
                .status(TicketStatus.NEW)
                .build();

        when(ticketRepository.findById(ticketId))
                .thenReturn(Optional.of(ticket));

        User otherUser = User.builder()
                .id(UUID.randomUUID())
                .email("other@example.com")
                .role(Role.CUSTOMER)
                .build();

        CustomUserDetails otherUserDetails =
                new CustomUserDetails(otherUser);

        assertThatThrownBy(
                () -> ticketService.findById(
                        ticketId,
                        otherUserDetails
                )
        )
                .isInstanceOf(AccessDeniedForTicketException.class);
    }

    @Test
    void findById_shouldThrowWhenTicketNotFound() {

        UUID ticketId = UUID.randomUUID();

        when(ticketRepository.findById(ticketId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> ticketService.findById(
                        ticketId,
                        customUserDetails
                )
        )
                .isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void findById_shouldReturnTicketForOperatorEvenWhenNotOwner()
            throws AccessDeniedForTicketException {

        UUID ticketId = UUID.randomUUID();

        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .user(user)
                .status(TicketStatus.NEW)
                .build();

        when(ticketRepository.findById(ticketId))
                .thenReturn(Optional.of(ticket));

        User operator = User.builder()
                .id(UUID.randomUUID())
                .email("operator@example.com")
                .role(Role.OPERATOR)
                .build();

        CustomUserDetails operatorDetails =
                new CustomUserDetails(operator);

        Ticket result = ticketService.findById(
                ticketId,
                operatorDetails
        );

        assertThat(result)
                .isEqualTo(ticket);
    }
}