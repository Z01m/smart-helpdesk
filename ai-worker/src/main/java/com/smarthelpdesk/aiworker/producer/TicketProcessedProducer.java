package com.smarthelpdesk.aiworker.producer;

import com.smarthelpdesk.aiworker.entity.AiProcessingResult;
import kafka.KafkaTopics;
import kafka.event.EventEnvelope;
import kafka.event.TicketProcessedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketProcessedProducer {

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public void send(AiProcessingResult result, String correlationId) {

        TicketProcessedEvent event = TicketProcessedEvent.builder()
                .ticketId(result.getTicketId())
                .processedAt(result.getProcessedAt())
                .answer(result.getAnswer())
                .category(result.getCategory())
                .priority(result.getPriority())
                .sentiment(result.getSentiment())
                .build();

        EventEnvelope<TicketProcessedEvent> envelope =
                EventEnvelope.<TicketProcessedEvent>builder()
                        .eventId(UUID.randomUUID())
                        .eventType("TICKET_PROCESSED")
                        .occurredAt(Instant.now())
                        .correlationId(correlationId)
                        .payload(event)
                        .build();

        String jsonPayload =
                objectMapper.writeValueAsString(envelope);

        ProducerRecord<String, String> record =
                new ProducerRecord<>(
                        KafkaTopics.TICKET_PROCESSED.getTopicName(),
                        result.getTicketId().toString(),
                        jsonPayload
                );

        if (correlationId != null) {
            record.headers().add(
                    "correlationId",
                    correlationId.getBytes(StandardCharsets.UTF_8)
            );
        }

        kafkaTemplate.send(record);

        log.info(
                "Sent ticket.processed event {} for ticket {}",
                envelope.getEventId(),
                result.getTicketId()
        );
    }
}