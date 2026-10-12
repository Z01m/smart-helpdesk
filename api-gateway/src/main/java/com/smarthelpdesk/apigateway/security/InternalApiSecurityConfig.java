package com.smarthelpdesk.apigateway.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class InternalApiSecurityConfig {
    @Bean
    @Order(1)
    SecurityFilterChain internalApiSecurity(HttpSecurity http,
            @Value("${internal.api-key:dev-internal-key}") String apiKey) throws Exception {
        if (apiKey.isBlank()) throw new IllegalArgumentException("Internal API key must not be blank");
        return http.securityMatcher("/internal/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().access((authentication, context) -> {
                    String provided = context.getRequest().getHeader("X-Internal-Api-Key");
                    return new AuthorizationDecision(provided != null && MessageDigest.isEqual(
                            apiKey.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8)));
                }))
                .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> res.sendError(401)))
                .build();
    }
}
