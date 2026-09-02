package com.smarthelpdesk.apigateway.dto.response;

import com.smarthelpdesk.apigateway.entity.enums.Priority;
import com.smarthelpdesk.apigateway.entity.enums.Sentiment;
import com.smarthelpdesk.apigateway.entity.enums.TicketStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Ответ API с информацией о конкретном тикете.
 * Содержит статус, категорию, приоритет, тональность и ответ AI.
 */

public record TicketResponse(
        UUID id,
        UUID userId,
        UUID operatorId,
        TicketStatus status,
        String message,
        String category,
        Priority priority,
        Sentiment sentiment,
        String generatedAnswer,
        Instant createdAt,
        Instant updatedAt
) {
}