package com.smarthelpdesk.apigateway.security;

import com.smarthelpdesk.apigateway.controller.InternalTicketController;
import com.smarthelpdesk.apigateway.service.TicketService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(InternalApiSecurityWebTest.Config.class)
@WebAppConfiguration
class InternalApiSecurityWebTest {
    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({InternalTicketController.class, InternalApiSecurityConfig.class})
    static class Config {}
    @Autowired WebApplicationContext context;
    @MockitoBean TicketService service;
    MockMvc mvc;
    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }
    @Test void ownerEndpointRequiresServiceKey() throws Exception {
        String path = "/internal/v1/tickets/" + UUID.randomUUID() + "/owner";
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).header("X-Internal-Api-Key", "wrong")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
    @Test void ownerEndpointReturnsMatchingContract() throws Exception {
        UUID ticket = UUID.randomUUID(), owner = UUID.randomUUID();
        when(service.findOwnerId(ticket)).thenReturn(owner);
        mvc.perform(get("/internal/v1/tickets/" + ticket + "/owner").header("X-Internal-Api-Key", "dev-internal-key"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ticketId").value(ticket.toString()))
                .andExpect(jsonPath("$.userId").value(owner.toString()));
    }
}
