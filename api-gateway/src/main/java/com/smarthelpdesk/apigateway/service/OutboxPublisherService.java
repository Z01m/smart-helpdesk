package com.smarthelpdesk.apigateway.service;

import com.smarthelpdesk.apigateway.entity.OutboxEvent;
import com.smarthelpdesk.apigateway.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class OutboxPublisherService {
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxEvent saveEvent(String aggregateType, UUID aggregateId, String eventType,  String correlationId,Object payload) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setAggregateType(aggregateType);
        outboxEvent.setEventType(eventType);
        outboxEvent.setCorrelationId(correlationId);
        outboxEvent.setPayload(objectMapper.writeValueAsString(payload));
        outboxEvent.setAggregateId(aggregateId);
        outboxEvent.setPublished(false);

        return outboxEventRepository.save(outboxEvent);
    }
}
