package com.smarthelpdesk.apigateway.kafka;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.smarthelpdesk.apigateway.entity.ProcessedEvent;
import com.smarthelpdesk.apigateway.repository.ProcessedEventRepository;
import com.smarthelpdesk.apigateway.service.TicketService;
import kafka.event.EventEnvelope;
import kafka.event.TicketProcessedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketProcessedConsumer {

    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;
    private final TicketService ticketService;

    @KafkaListener(
            topics = "ticket.processed",
            groupId = "api-gateway"
    )
    public void consume(
            ConsumerRecord<String, String> record,
            Acknowledgment acknowledgment
    ) {
        try {
            var correlationHeader =
                    record.headers().lastHeader("correlationId");

            if (correlationHeader != null) {
                String correlationId = new String(
                        correlationHeader.value(),
                        StandardCharsets.UTF_8
                );

                MDC.put("correlationId", correlationId);
            }

            EventEnvelope<TicketProcessedEvent> envelope =
                    objectMapper.readValue(
                            record.value(),
                            new TypeReference<EventEnvelope<TicketProcessedEvent>>() {
                            }
                    );

            if (processedEventRepository.existsByEventId(envelope.getEventId())) {
                log.info(
                        "Event {} already processed, skipping",
                        envelope.getEventId()
                );

                acknowledgment.acknowledge();
                return;
            }

            TicketProcessedEvent payload = envelope.getPayload();

            ticketService.applyAiResult(
                    payload.ticketId(),
                    payload.category(),
                    payload.priority(),
                    payload.sentiment(),
                    payload.answer()
            );

            ProcessedEvent processedEvent = ProcessedEvent.builder()
                    .eventId(envelope.getEventId())
                    .processedAt(Instant.now())
                    .build();

            processedEventRepository.save(processedEvent);

            acknowledgment.acknowledge();

            log.info(
                    "Successfully processed ticket.processed event {} for ticket {}",
                    envelope.getEventId(),
                    payload.ticketId()
            );

        } catch (Exception exception) {
            log.error(
                    "Failed to process ticket.processed message. Topic: {}, partition: {}, offset: {}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    exception
            );

            throw new RuntimeException(exception);

        } finally {
            MDC.remove("correlationId");
        }
    }
}
