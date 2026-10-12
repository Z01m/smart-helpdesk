package com.smarthelpdesk.apigateway.servise;

import com.smarthelpdesk.apigateway.entity.*;
import com.smarthelpdesk.apigateway.entity.enums.*;
import com.smarthelpdesk.apigateway.repository.*;
import com.smarthelpdesk.apigateway.service.ProcessedTicketService;
import com.smarthelpdesk.apigateway.mapper.TicketMapper;
import com.smarthelpdesk.apigateway.kafka.OutboxEventPoller;
import kafka.event.TicketProcessedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
@SpringBootTest(properties="spring.kafka.listener.auto-startup=false")
class GatewayDatabaseIntegrationTest {
    @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",postgres::getJdbcUrl);
        r.add("spring.datasource.username",postgres::getUsername);
        r.add("spring.datasource.password",postgres::getPassword);
    }
    @Autowired ProcessedTicketService service;
    @Autowired TicketRepository tickets;
    @Autowired UserRepository users;
    @Autowired TicketMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean OutboxEventPoller outbox;
    Ticket ticket() {
        User user=users.save(User.builder().email(UUID.randomUUID()+"@example.test").passwordHash("unused")
            .fullName("Test").role(Role.CUSTOMER).customerTier(CustomerTier.STANDARD).build());
        return tickets.save(Ticket.builder().user(user).message("Question").status(TicketStatus.NEW).build());
    }
    @Test void persistsLongAnswerAndDeduplicatesAtomically() {
        var ticket=ticket();UUID event=UUID.randomUUID();String answer="Long answer. ".repeat(300);
        var payload=TicketProcessedEvent.builder().ticketId(ticket.getId()).category("TECHNICAL")
            .priority("NORMAL").sentiment("NEUTRAL").generatedAnswer(answer).requiresHumanReview(false).build();
        service.process(event,payload);service.process(event,payload);
        var saved=tickets.findById(ticket.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(TicketStatus.ANSWED);
        assertThat(saved.getPriority()).isEqualTo(Priority.NORMAL);
        // Populate lazy association for the mapper without a persistence context.
        saved.setUser(ticket.getUser());
        assertThat(mapper.toResponse(saved).generatedAnswer()).isEqualTo(answer);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_status_history WHERE ticket_id=?",Long.class,ticket.getId())).isEqualTo(3);
    }
    @Test void invalidResultRollsBackClaimAndCanBeRetried() {
        var ticket=ticket();UUID event=UUID.randomUUID();
        var bad=TicketProcessedEvent.builder().ticketId(ticket.getId()).category("TECHNICAL")
            .priority("INVALID").sentiment("NEUTRAL").requiresHumanReview(true).build();
        assertThatThrownBy(()->service.process(event,bad)).isInstanceOf(IllegalArgumentException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM processed_event WHERE event_id=?",Long.class,event)).isZero();
        assertThat(tickets.findById(ticket.getId()).orElseThrow().getStatus()).isEqualTo(TicketStatus.NEW);
        service.process(event,TicketProcessedEvent.builder().ticketId(ticket.getId()).category("TECHNICAL")
            .priority("HIGH").sentiment("ANGRY").requiresHumanReview(true).build());
        assertThat(tickets.findById(ticket.getId()).orElseThrow().getStatus()).isEqualTo(TicketStatus.WAITIND_OPERATOR);
    }
}
