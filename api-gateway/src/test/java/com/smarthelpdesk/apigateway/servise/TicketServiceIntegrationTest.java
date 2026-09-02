package com.smarthelpdesk.apigateway.servise;

import com.smarthelpdesk.apigateway.entity.OutboxEvent;
import com.smarthelpdesk.apigateway.entity.Ticket;
import com.smarthelpdesk.apigateway.entity.User;
import com.smarthelpdesk.apigateway.entity.enums.CustomerTier;
import com.smarthelpdesk.apigateway.entity.enums.Role;
import com.smarthelpdesk.apigateway.entity.enums.TicketStatus;
import com.smarthelpdesk.apigateway.repository.OutboxEventRepository;
import com.smarthelpdesk.apigateway.repository.TicketRepository;
import com.smarthelpdesk.apigateway.repository.UserRepository;
import com.smarthelpdesk.apigateway.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TicketServiceIntegrationTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createTicket_shouldSaveTicketAndOutboxEvent() throws Exception {

        // ARRANGE
        User user = User.builder()
                .email("outbox-test-" + UUID.randomUUID() + "@test.com")
                .passwordHash("password-hash")
                .fullName("Test User")
                .role(Role.CUSTOMER)
                .customerTier(CustomerTier.STANDARD)
                .build();

        User savedUser = userRepository.save(user);

        String message = "Не могу войти в личный кабинет";


        // ACT
        Ticket createdTicket = ticketService.createTicket(
                message,
                savedUser.getId()
        );


        // ASSERT: Ticket
        assertThat(createdTicket.getId()).isNotNull();
        assertThat(createdTicket.getMessage()).isEqualTo(message);
        assertThat(createdTicket.getStatus()).isEqualTo(TicketStatus.NEW);
        assertThat(createdTicket.getUser().getId()).isEqualTo(savedUser.getId());

        Ticket ticketFromDatabase = ticketRepository
                .findById(createdTicket.getId())
                .orElseThrow();

        assertThat(ticketFromDatabase.getMessage()).isEqualTo(message);
        assertThat(ticketFromDatabase.getStatus()).isEqualTo(TicketStatus.NEW);


        // ASSERT: Outbox
        List<OutboxEvent> unpublishedEvents =
                outboxEventRepository.findByPublishedFalse();

        OutboxEvent outboxEvent = unpublishedEvents.stream()
                .filter(event ->
                        event.getAggregateId().equals(createdTicket.getId())
                )
                .findFirst()
                .orElseThrow();

        assertThat(outboxEvent.getId()).isNotNull();

        assertThat(outboxEvent.getAggregateType())
                .isEqualTo("Ticket");

        assertThat(outboxEvent.getAggregateId())
                .isEqualTo(createdTicket.getId());

        assertThat(outboxEvent.getEventType())
                .isEqualTo("TICKET_CREATED");

        assertThat(outboxEvent.isPublished())
                .isFalse();

        assertThat(outboxEvent.getPublishedAt())
                .isNull();

        assertThat(outboxEvent.getCreatedAt())
                .isNotNull();


        // ASSERT: JSON payload
        JsonNode payload =
                objectMapper.readTree(outboxEvent.getPayload());

        assertThat(payload.get("eventId").asText())
                .isNotBlank();

        assertThat(payload.get("ticketId").asText())
                .isEqualTo(createdTicket.getId().toString());

        assertThat(payload.get("userId").asText())
                .isEqualTo(savedUser.getId().toString());

        assertThat(payload.get("message").asText())
                .isEqualTo(message);

        assertThat(payload.get("customerTier").asText())
                .isEqualTo(CustomerTier.STANDARD.name());

        assertThat(payload.get("createdAt"))
                .isNotNull();
    }
}