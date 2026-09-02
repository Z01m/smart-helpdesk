package com.smarthelpdesk.apigateway.mapper;

import com.smarthelpdesk.apigateway.entity.enums.Priority;
import com.smarthelpdesk.apigateway.entity.enums.Sentiment;
import com.smarthelpdesk.apigateway.entity.enums.TicketStatus;

import java.time.Instant;
import java.util.UUID;
/**
 * Преобразует User между Entity и DTO.
 * Используется при формировании ответов API и работе с запросами.
 */
public record UserMapper(
        UUID id,
        TicketStatus status,
        String message,
        Priority priority,
        Sentiment sentiment,
        String generatedAnswer,
        UUID operatorId,
        Instant createdAt,
        Instant updatedAt
) {
}
