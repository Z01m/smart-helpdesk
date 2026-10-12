package com.smarthelpdesk.aiworker.service;
import com.smarthelpdesk.aiworker.repository.ProcessedEventRepository;
import com.smarthelpdesk.aiworker.producer.TicketProcessedProducer;
import kafka.event.TicketCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketEventProcessingService {
    private final ProcessedEventRepository events;
    private final TicketProcessingOrchestrator orchestrator;
    private final TicketProcessedProducer producer;
    private final JdbcTemplate jdbc;

    @Transactional
    public void process(UUID eventId, TicketCreatedEvent event, String correlationId) {
        if(eventId==null || event==null || event.ticketId()==null || event.message()==null || event.message().isBlank())
            throw new IllegalArgumentException("Event id, ticket id and message are required");
        if(events.claim(eventId)==0)return;
        var outcome=orchestrator.process(event);
        var record=producer.record(outcome.ticketResult(),outcome.requiresHumanReview(),correlationId,eventId);
        // Result, input claim and output event commit together. Kafka outages cannot lose the answer.
        jdbc.update("INSERT INTO result_outbox(id,ticket_id,payload,correlation_id) VALUES (?,?,?,?)",eventId,event.ticketId(),record.value(),
                new String(record.headers().lastHeader("correlationId").value(),java.nio.charset.StandardCharsets.UTF_8));
    }
}
