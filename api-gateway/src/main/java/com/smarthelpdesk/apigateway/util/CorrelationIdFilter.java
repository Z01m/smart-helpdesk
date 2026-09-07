package com.smarthelpdesk.apigateway.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("X-Correlation-Id");
        if (header == null || header.isBlank()) {
            header = UUID.randomUUID().toString();
        }
        MDC.put("correlationId", header);
        try {
            filterChain.doFilter(request, response);
        }finally {
            MDC.remove("correlationId");
        }

    }
}
