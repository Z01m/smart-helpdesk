package com.smarthelpdesk.apigateway.repository;


import com.smarthelpdesk.apigateway.entity.Ticket;
import com.smarthelpdesk.apigateway.entity.User;
import com.smarthelpdesk.apigateway.entity.enums.CustomerTier;
import com.smarthelpdesk.apigateway.entity.enums.Role;
import com.smarthelpdesk.apigateway.entity.enums.TicketStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
public class TicketRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("gateway")
            .withUsername("helpdesk")
            .withPassword("helpdesk");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry){
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
    }

    @Autowired

    private UserRepository userRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    @Transactional
    void shouldSaveAndRetrieveTicketWithUser() {
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("hashed-password")
                .fullName("Ivan Petrov")
                .role(Role.CUSTOMER)
                .customerTier(CustomerTier.STANDARD)
                .build();
        User savedUser = userRepository.save(user);

        Ticket ticket = Ticket.builder()
                .user(savedUser)
                .message("Не приходит письмо для сброса пароля")
                .status(TicketStatus.NEW)
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);
        Ticket foundTicket = ticketRepository.findById(savedTicket.getId()).orElseThrow();

        assertThat(foundTicket.getMessage()).isEqualTo("Не приходит письмо для сброса пароля");
        assertThat(foundTicket.getStatus()).isEqualTo(TicketStatus.NEW);
        assertThat(foundTicket.getUser().getEmail()).isEqualTo("test@example.com");
        assertThat(foundTicket.getVersion()).isEqualTo(0);
    }

}
