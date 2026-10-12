package com.smarthelpdesk.apigateway.service;

import com.smarthelpdesk.apigateway.repository.ProcessedEventRepository;
import kafka.event.TicketProcessedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessedTicketService {
    private final ProcessedEventRepository events;
    private final TicketService tickets;

    // Claim and ticket/history changes commit together; failures permit Kafka redelivery.
    @Transactional
    public void process(UUID eventId, TicketProcessedEvent payload) {
        if (eventId == null || payload == null || payload.ticketId() == null)
            throw new IllegalArgumentException("Event and ticket identifiers are required");
        if (events.claim(eventId) == 0) return;
        tickets.applyAiResult(payload.ticketId(), payload.category(), payload.priority(), payload.sentiment(),
                payload.generatedAnswer(), payload.requiresHumanReview());
    }
}
