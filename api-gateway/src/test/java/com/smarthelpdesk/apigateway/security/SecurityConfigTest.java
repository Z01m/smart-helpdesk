package com.smarthelpdesk.apigateway.security;

import com.smarthelpdesk.apigateway.mapper.TicketMapper;
import com.smarthelpdesk.apigateway.security.JwtAuthenticationFilter;
import com.smarthelpdesk.apigateway.security.JwtTokenProvider;

import com.smarthelpdesk.apigateway.service.CustomUserDetailsService;
import com.smarthelpdesk.apigateway.service.TicketService;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private TicketMapper ticketMapper;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void protectedEndpoint_shouldReturn401_whenUserNotAuthenticated()
            throws Exception {

        mockMvc.perform(
                        get("/some-protected-url")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_shouldPassSecurity_whenUserAuthenticated()
            throws Exception {

        mockMvc.perform(
                        get("/some-protected-url")
                                .with(user("test@example.com"))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void healthEndpoint_shouldNotRequireAuthentication()
            throws Exception {

        mockMvc.perform(
                        get("/actuator/health")
                )
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();

                    if (status == 401 || status == 403) {
                        throw new AssertionError(
                                "Health endpoint must be publicly accessible"
                        );
                    }
                });
    }
}
