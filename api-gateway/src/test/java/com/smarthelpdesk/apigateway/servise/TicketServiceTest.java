package com.smarthelpdesk.apigateway.servise;

import com.smarthelpdesk.apigateway.entity.Ticket;
import com.smarthelpdesk.apigateway.entity.TicketStatusHistory;
import com.smarthelpdesk.apigateway.entity.User;
import com.smarthelpdesk.apigateway.entity.enums.CustomerTier;
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
import com.smarthelpdesk.apigateway.service.OutboxPublisherService;
import com.smarthelpdesk.apigateway.service.TicketService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    private MockMvc mockMvc;

    private User user;
    private UUID userId;
    private CustomUserDetails customUserDetails;

    private static Stream<Arguments> allowedTransitions() {
        return Stream.of(
                Arguments.of(TicketStatus.NEW, TicketStatus.PROCESSING),

                Arguments.of(TicketStatus.PROCESSING, TicketStatus.CLASSIFIED),
                Arguments.of(TicketStatus.PROCESSING, TicketStatus.FAILED),

                Arguments.of(TicketStatus.CLASSIFIED, TicketStatus.ANSWED),
                Arguments.of(TicketStatus.CLASSIFIED, TicketStatus.WAITIND_OPERATOR),

                Arguments.of(TicketStatus.FAILED, TicketStatus.WAITIND_OPERATOR),

                Arguments.of(TicketStatus.ANSWED, TicketStatus.CLOSED),
                Arguments.of(TicketStatus.ANSWED, TicketStatus.WAITIND_OPERATOR),

                Arguments.of(TicketStatus.WAITIND_OPERATOR, TicketStatus.ANSWED),
                Arguments.of(TicketStatus.WAITIND_OPERATOR, TicketStatus.CLOSED),

                Arguments.of(TicketStatus.CLOSED, TicketStatus.REOPENED),

                Arguments.of(TicketStatus.REOPENED, TicketStatus.PROCESSING)
        );
    }

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

    private CustomUserDetails createOperatorDetails() {
        User operator = User.builder()
                .id(UUID.randomUUID())
                .email("operator@example.com")
                .role(Role.OPERATOR)
                .build();

        return new CustomUserDetails(operator);
    }

    @Test
    void updateStatus_shouldChangeStatus_fromNewToProcessing() throws AccessDeniedForTicketException {
        UUID ticketId = UUID.randomUUID();

        CustomUserDetails currentUser = createOperatorDetails();

        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .status(TicketStatus.NEW)
                .build();

        when(ticketRepository.findById(ticketId))
                .thenReturn(Optional.of(ticket));

        when(ticketRepository.save(ticket))
                .thenReturn(ticket);

        Ticket result = ticketService.updateStatus(
                ticketId,
                TicketStatus.PROCESSING,
                currentUser
        );

        assertEquals(TicketStatus.PROCESSING, result.getStatus());

        verify(ticketStatusHistoryRepository).save(any(TicketStatusHistory.class));
        verify(ticketRepository).save(ticket);
    }

    @Test
    void updateStatus_shouldThrowInvalidStatusTransition_fromNewToClosed() {
        UUID ticketId = UUID.randomUUID();

        CustomUserDetails currentUser = createOperatorDetails();

        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .status(TicketStatus.NEW)
                .build();

        when(ticketRepository.findById(ticketId))
                .thenReturn(Optional.of(ticket));

        assertThrows(
                InvalidStatusTransitionException.class,
                () -> ticketService.updateStatus(
                        ticketId,
                        TicketStatus.CLOSED,
                        currentUser
                )
        );

        verify(ticketStatusHistoryRepository, never()).save(any());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void updateStatus_shouldThrowAccessDenied_whenCurrentUserIsCustomer() {
        UUID ticketId = UUID.randomUUID();

        User customer = User.builder()
                .id(UUID.randomUUID())
                .email("customer@example.com")
                .role(Role.CUSTOMER)
                .build();

        CustomUserDetails currentUser =
                new CustomUserDetails(customer);

        assertThrows(
                AccessDeniedForTicketException.class,
                () -> ticketService.updateStatus(
                        ticketId,
                        TicketStatus.PROCESSING,
                        currentUser
                )
        );

        verify(ticketRepository, never()).findById(any());
        verify(ticketStatusHistoryRepository, never()).save(any());
        verify(ticketRepository, never()).save(any());
    }


    @Test
    void updateStatus_shouldReturnTicketWithoutSavingHistory_whenStatusDoesNotChange() throws AccessDeniedForTicketException {
        UUID ticketId = UUID.randomUUID();

        CustomUserDetails currentUser = createOperatorDetails();

        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .status(TicketStatus.NEW)
                .build();

        when(ticketRepository.findById(ticketId))
                .thenReturn(Optional.of(ticket));

        Ticket result = ticketService.updateStatus(
                ticketId,
                TicketStatus.NEW,
                currentUser
        );

        assertEquals(TicketStatus.NEW, result.getStatus());

        verify(ticketStatusHistoryRepository, never()).save(any());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void updateStatus_shouldSaveHistoryWithCurrentUserId() throws AccessDeniedForTicketException {
        UUID ticketId = UUID.randomUUID();

        User operator = User.builder()
                .id(UUID.randomUUID())
                .email("operator@example.com")
                .role(Role.OPERATOR)
                .build();

        CustomUserDetails currentUser =
                new CustomUserDetails(operator);

        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .status(TicketStatus.NEW)
                .build();

        when(ticketRepository.findById(ticketId))
                .thenReturn(Optional.of(ticket));

        when(ticketRepository.save(ticket))
                .thenReturn(ticket);

        ArgumentCaptor<TicketStatusHistory> historyCaptor =
                ArgumentCaptor.forClass(TicketStatusHistory.class);

        ticketService.updateStatus(
                ticketId,
                TicketStatus.PROCESSING,
                currentUser
        );

        verify(ticketStatusHistoryRepository)
                .save(historyCaptor.capture());

        TicketStatusHistory history =
                historyCaptor.getValue();

        assertEquals(TicketStatus.NEW, history.getFromStatus());
        assertEquals(TicketStatus.PROCESSING, history.getToStatus());
        assertEquals(operator.getId(), history.getChangedBy());
    }

    @ParameterizedTest
    @MethodSource("allowedTransitions")
    void updateStatus_shouldAllowValidTransitions(
            TicketStatus from,
            TicketStatus to
    ) throws AccessDeniedForTicketException {
        UUID ticketId = UUID.randomUUID();

        CustomUserDetails currentUser = createOperatorDetails();

        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .status(from)
                .build();

        when(ticketRepository.findById(ticketId))
                .thenReturn(Optional.of(ticket));

        when(ticketRepository.save(ticket))
                .thenReturn(ticket);

        Ticket result = ticketService.updateStatus(
                ticketId,
                to,
                currentUser
        );

        assertEquals(to, result.getStatus());

        verify(ticketStatusHistoryRepository).save(any(TicketStatusHistory.class));
        verify(ticketRepository).save(ticket);
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
    void assignOperator_shouldAssignOperator_whenCurrentUserIsOperator() {
        UUID ticketId = UUID.randomUUID();
        UUID operatorId = UUID.randomUUID();

        User ticketOwner = User.builder()
                .id(UUID.randomUUID())
                .email("customer@example.com")
                .role(Role.CUSTOMER)
                .build();

        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .user(ticketOwner)
                .status(TicketStatus.NEW)
                .build();

        User operator = User.builder()
                .id(operatorId)
                .email("operator@example.com")
                .role(Role.OPERATOR)
                .build();

        CustomUserDetails currentUser = new CustomUserDetails(operator);

        when(ticketRepository.findById(ticketId))
                .thenReturn(Optional.of(ticket));

        when(userRepository.findById(operatorId))
                .thenReturn(Optional.of(operator));

        when(ticketRepository.save(ticket))
                .thenReturn(ticket);

        Ticket result = ticketService.assignOperator(
                ticketId,
                operatorId,
                currentUser
        );

        assertNotNull(result);
        assertEquals(operator, result.getOperator());
        assertEquals(operatorId, result.getOperator().getId());

        verify(ticketRepository).findById(ticketId);
        verify(userRepository).findById(operatorId);
        verify(ticketRepository).save(ticket);
    }

    @Test
    void assignOperator_shouldThrowAccessDenied_whenCurrentUserIsCustomer() {
        UUID ticketId = UUID.randomUUID();
        UUID operatorId = UUID.randomUUID();

        User customer = User.builder()
                .id(UUID.randomUUID())
                .email("customer@example.com")
                .role(Role.CUSTOMER)
                .build();

        CustomUserDetails currentUser = new CustomUserDetails(customer);

        assertThrows(
                AccessDeniedException.class,
                () -> ticketService.assignOperator(
                        ticketId,
                        operatorId,
                        currentUser
                )
        );

        verify(ticketRepository, never()).findById(any());
        verify(userRepository, never()).findById(any());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void assignOperator_shouldThrowAccessDenied_whenSelectedUserIsNotOperator() {
        UUID ticketId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        User currentOperator = User.builder()
                .id(UUID.randomUUID())
                .email("operator@example.com")
                .role(Role.OPERATOR)
                .build();

        CustomUserDetails currentUser =
                new CustomUserDetails(currentOperator);

        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .status(TicketStatus.NEW)
                .build();

        User customer = User.builder()
                .id(customerId)
                .email("customer@example.com")
                .role(Role.CUSTOMER)
                .build();

        when(ticketRepository.findById(ticketId))
                .thenReturn(Optional.of(ticket));

        when(userRepository.findById(customerId))
                .thenReturn(Optional.of(customer));

        assertThrows(
                AccessDeniedException.class,
                () -> ticketService.assignOperator(
                        ticketId,
                        customerId,
                        currentUser
                )
        );

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void assignOperator_shouldThrowTicketNotFound_whenTicketDoesNotExist() {
        UUID ticketId = UUID.randomUUID();
        UUID operatorId = UUID.randomUUID();

        User currentOperator = User.builder()
                .id(UUID.randomUUID())
                .email("operator@example.com")
                .role(Role.OPERATOR)
                .build();

        CustomUserDetails currentUser =
                new CustomUserDetails(currentOperator);

        when(ticketRepository.findById(ticketId))
                .thenReturn(Optional.empty());

        assertThrows(
                TicketNotFoundException.class,
                () -> ticketService.assignOperator(
                        ticketId,
                        operatorId,
                        currentUser
                )
        );

        verify(userRepository, never()).findById(any());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void assignOperator_shouldThrowUserNotFound_whenOperatorDoesNotExist() {
        UUID ticketId = UUID.randomUUID();
        UUID operatorId = UUID.randomUUID();

        User currentOperator = User.builder()
                .id(UUID.randomUUID())
                .email("operator@example.com")
                .role(Role.OPERATOR)
                .build();

        CustomUserDetails currentUser =
                new CustomUserDetails(currentOperator);

        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .status(TicketStatus.NEW)
                .build();

        when(ticketRepository.findById(ticketId))
                .thenReturn(Optional.of(ticket));

        when(userRepository.findById(operatorId))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> ticketService.assignOperator(
                        ticketId,
                        operatorId,
                        currentUser
                )
        );

        verify(ticketRepository, never()).save(any());
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