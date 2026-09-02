package com.smarthelpdesk.apigateway.controller;

import com.smarthelpdesk.apigateway.dto.request.CreateTicketRequest;
import com.smarthelpdesk.apigateway.entity.Ticket;
import com.smarthelpdesk.apigateway.entity.User;
import com.smarthelpdesk.apigateway.entity.enums.Role;
import com.smarthelpdesk.apigateway.entity.enums.TicketStatus;
import com.smarthelpdesk.apigateway.exception.AccessDeniedForTicketException;
import com.smarthelpdesk.apigateway.exception.TicketNotFoundException;
import com.smarthelpdesk.apigateway.mapper.TicketMapperImpl;
import com.smarthelpdesk.apigateway.security.CustomUserDetails;
import com.smarthelpdesk.apigateway.security.JwtTokenProvider;
import com.smarthelpdesk.apigateway.service.CustomUserDetailsService;
import com.smarthelpdesk.apigateway.service.TicketService;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import tools.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request
        .SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request
        .MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request
        .MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result
        .MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result
        .MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result
        .MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
@Import(TicketMapperImpl.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private UUID userId;
    private UUID ticketId;

    private User user;
    private CustomUserDetails customUserDetails;
    private Authentication authentication;

    @BeforeEach
    void setUp() {

        userId = UUID.randomUUID();
        ticketId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .email("test@example.com")
                .role(Role.CUSTOMER)
                .build();

        customUserDetails = new CustomUserDetails(user);
        authentication =
                new UsernamePasswordAuthenticationToken(
                        customUserDetails,
                        null,
                        customUserDetails.getAuthorities()
                );

    }

    @Test
    void createTicket_shouldReturn201() throws Exception {

        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .user(user)
                .message("Проблема с оплатой")
                .status(TicketStatus.NEW)
                .build();

        /*
         * Здесь нас интересует не Mockito-проверка аргументов,
         * а HTTP-поведение контроллера.
         *
         * Поэтому говорим:
         * если сервис вызвали со String + UUID,
         * верни наш Ticket.
         */
        when(ticketService.createTicket(
                anyString(),
                any(UUID.class)
        )).thenReturn(ticket);

        mockMvc.perform(
                        post("/api/v1/tickets")
                                .principal(authentication)
                                .contentType("application/json")
                                .content(
                                        objectMapper.writeValueAsString(
                                                new CreateTicketRequest(
                                                        "Проблема с оплатой"
                                                )
                                        )
                                )
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id")
                        .value(ticketId.toString()))
                .andExpect(jsonPath("$.userId")
                        .value(userId.toString()))
                .andExpect(jsonPath("$.message")
                        .value("Проблема с оплатой"))
                .andExpect(jsonPath("$.status")
                        .value("NEW"));

        /*
         * А вот здесь уже отдельно проверяем,
         * что Controller действительно передал правильные значения.
         */
        verify(ticketService).createTicket(
                "Проблема с оплатой",
                userId
        );
    }

    @Test
    void createTicket_shouldReturn400WhenMessageBlank()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/tickets")
                                .principal(authentication)
                                .contentType("application/json")
                                .content(
                                        objectMapper.writeValueAsString(
                                                new CreateTicketRequest("")
                                        )
                                )
                )
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTicketById_shouldReturn200() throws Exception {

        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .user(user)
                .message("Проблема с оплатой")
                .status(TicketStatus.NEW)
                .build();

        when(ticketService.findById(
                eq(ticketId),
                any(CustomUserDetails.class)
        )).thenReturn(ticket);

        mockMvc.perform(
                        get("/api/v1/tickets/{id}", ticketId)
                                .principal(authentication)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(ticketId.toString()))
                .andExpect(jsonPath("$.userId")
                        .value(userId.toString()))
                .andExpect(jsonPath("$.message")
                        .value("Проблема с оплатой"))
                .andExpect(jsonPath("$.status")
                        .value("NEW"));

        verify(ticketService).findById(
                eq(ticketId),
                any(CustomUserDetails.class)
        );
    }

    @Test
    void getTicketById_shouldReturn404WhenTicketNotFound()
            throws Exception {

        when(ticketService.findById(
                eq(ticketId),
                any(CustomUserDetails.class)
        )).thenThrow(
                new TicketNotFoundException(ticketId)
        );

        mockMvc.perform(
                        get("/api/v1/tickets/{id}", ticketId)
                                .principal(authentication)
                )
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(ticketService).findById(
                eq(ticketId),
                any(CustomUserDetails.class)
        );
    }

    @Test
    void getTicketById_shouldReturn403WhenAccessDenied()
            throws Exception {

        when(ticketService.findById(
                eq(ticketId),
                any(CustomUserDetails.class)
        )).thenThrow(
                new AccessDeniedForTicketException(ticketId)
        );

        mockMvc.perform(
                        get("/api/v1/tickets/{id}", ticketId)
                                .principal(authentication)
                )
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(ticketService).findById(
                eq(ticketId),
                any(CustomUserDetails.class)
        );
    }
}