package com.smarthelpdesk.apigateway.kafka;
import kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
@Component
@RequiredArgsConstructor
public class TicketEventProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    public CompletableFuture<SendResult<String, String>> send(UUID ticketId, String correlationId, String payload) {
        return send(KafkaTopics.TICKET_CREATED, ticketId, correlationId, payload);
    }
    public CompletableFuture<SendResult<String, String>> send(KafkaTopics topic, UUID ticketId, String correlationId, String payload) {
        var record = new ProducerRecord<String, String>(topic.getTopicName(), ticketId.toString(), payload);
        if (correlationId != null) record.headers().add("correlationId", correlationId.getBytes(StandardCharsets.UTF_8));
        return kafkaTemplate.send(record);
    }
}
