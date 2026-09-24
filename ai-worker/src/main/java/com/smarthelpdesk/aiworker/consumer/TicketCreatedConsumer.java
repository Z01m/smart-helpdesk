package com.smarthelpdesk.aiworker.consumer;

import com.smarthelpdesk.aiworker.entity.AiProcessingResult;
import com.smarthelpdesk.aiworker.entity.ProcessedEvent;
import com.smarthelpdesk.aiworker.entity.TicketResult;
import com.smarthelpdesk.aiworker.producer.TicketProcessedProducer;
import com.smarthelpdesk.aiworker.repository.ProcessedEventRepository;
import com.smarthelpdesk.aiworker.service.TicketProcessingOrchestrator;
import kafka.event.EventEnvelope;
import kafka.event.TicketCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketCreatedConsumer {

    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;
    private final TicketProcessingOrchestrator ticketProcessingOrchestrator;
    private final TicketProcessedProducer ticketProcessedProducer;

    @KafkaListener(
            topics = "ticket.created",
            groupId = "ai-worker"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {

            String correlationId = null;

            var correlationHeader = record.headers().lastHeader("correlationId");

            if (correlationHeader != null) {

                correlationId = new String(correlationHeader.value(), StandardCharsets.UTF_8);

                MDC.put("correlationId", correlationId);
            }

            EventEnvelope<TicketCreatedEvent> envelope =
                    objectMapper.readValue(record.value(), new TypeReference<EventEnvelope<TicketCreatedEvent>>() {});

            UUID eventId = envelope.getEventId();

            if (processedEventRepository.existsByEventId(eventId)) {

                log.info("Event {} already processed, skipping", eventId);

                acknowledgment.acknowledge();

                return;
            }

            TicketCreatedEvent payload = envelope.getPayload();

            TicketResult result = ticketProcessingOrchestrator.process(payload);

            ticketProcessedProducer.send(result, correlationId);

            ProcessedEvent processedEvent = ProcessedEvent.builder()
                            .eventId(eventId)
                            .processedAt(Instant.now())
                            .build();

            processedEventRepository.save(processedEvent);

            acknowledgment.acknowledge();

            log.info("Successfully processed ticket.created event {} for ticket {}", eventId, payload.ticketId());

        } catch (Exception exception) {

            log.error("Failed to process ticket.created message. Topic: {}, partition: {}, offset: {}", record.topic(), record.partition(), record.offset(), exception
            );

            throw new RuntimeException(exception);

        } finally {
            MDC.remove("correlationId");
        }
    }
}