package com.smarthelpdesk.aiworker.producer;

import com.smarthelpdesk.aiworker.entity.TicketResult;
import kafka.KafkaTopics;
import kafka.event.EventEnvelope;
import kafka.event.TicketProcessedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketProcessedProducer {

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public void send(
            TicketResult result,
            String correlationId
    ) {

        if (result == null) {
            throw new IllegalArgumentException("result cannot be null");
        }

        String actualCorrelationId =
                correlationId == null || correlationId.isBlank()
                        ? UUID.randomUUID().toString()
                        : correlationId;

        TicketProcessedEvent event =
                TicketProcessedEvent.builder()
                        .ticketId(result.getTicketId())
                        .category(result.getCategory())
                        .priority(result.getPriority())
                        .sentiment(result.getSentiment())
                        .confidence(result.getConfidence())
                        .generatedAnswer(result.getGeneratedAnswer())
                        .contextSources(
                                deserializeContextSources(
                                        result.getContextSources()
                                )
                        )
                        .processedAt(result.getCreatedAt())
                        .build();

        EventEnvelope<TicketProcessedEvent> envelope =
                EventEnvelope.<TicketProcessedEvent>builder()
                        .eventId(UUID.randomUUID())
                        .eventType("TICKET_PROCESSED")
                        .occurredAt(result.getCreatedAt())
                        .correlationId(actualCorrelationId)
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

        record.headers().add(
                "correlationId",
                actualCorrelationId.getBytes(StandardCharsets.UTF_8)
        );

        kafkaTemplate.send(record);

        log.info(
                "Sent ticket.processed event {} for ticket {}",
                envelope.getEventId(),
                result.getTicketId()
        );
    }

    private List<TicketProcessedEvent.ContextSource>
    deserializeContextSources(String json) {

        if (json == null || json.isBlank()) {
            return List.of();
        }

        return objectMapper.readValue(
                json,
                new TypeReference<
                        List<TicketProcessedEvent.ContextSource>
                        >() {
                }
        );
    }
}