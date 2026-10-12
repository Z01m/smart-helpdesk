package com.smarthelpdesk.notificationservice.kafka;

import com.smarthelpdesk.notificationservice.service.NotificationService;
import kafka.event.*;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class TicketProcessedConsumer {
    private final ObjectMapper mapper;
    private final NotificationService service;

    @KafkaListener(topics = "ticket.processed", groupId = "notification-service")
    public void consume(String json, Acknowledgment acknowledgment) {
        EventEnvelope<TicketProcessedEvent> event = mapper.readValue(json, new TypeReference<>() {});
        if (event == null) throw new IllegalArgumentException("Event envelope is required");
        service.process(event.getEventId(), event.getPayload());
        // The proxied service has committed its database transaction before the offset is acknowledged.
        acknowledgment.acknowledge();
    }
}
