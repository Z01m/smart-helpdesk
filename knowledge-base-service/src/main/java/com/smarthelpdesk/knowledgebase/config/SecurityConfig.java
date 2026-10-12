package com.smarthelpdesk.knowledgebase.config;

import com.smarthelpdesk.knowledgebase.security.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean
    @Order(1)
    SecurityFilterChain internalSecurity(HttpSecurity http, @Value("${internal.api-key}") String key) throws Exception {
        if (key.isBlank()) throw new IllegalArgumentException("Internal API key must not be blank");
        return http.securityMatcher("/internal/**").csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.anyRequest().access((auth, context) -> {
                    String provided = context.getRequest().getHeader("X-Internal-Api-Key");
                    return new AuthorizationDecision(provided != null && MessageDigest.isEqual(
                            key.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8)));
                }))
                .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> res.sendError(401))).build();
    }
    @Bean
    SecurityFilterChain publicSecurity(HttpSecurity http, AccessTokenVerifier verifier) throws Exception {
        return http.csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/articles/**", "/api/v1/categories/**").authenticated()
                        .requestMatchers("/api/v1/articles/**", "/api/v1/categories/**").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> res.sendError(401)))
                .addFilterBefore(new AccessTokenFilter(verifier), UsernamePasswordAuthenticationFilter.class).build();
    }
}
