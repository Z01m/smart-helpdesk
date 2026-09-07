package com.smarthelpdesk.apigateway.kafka;

import com.smarthelpdesk.apigateway.entity.Ticket;
import kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


@Component
@RequiredArgsConstructor
@Slf4j
public class TicketEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

/*    public CompletableFuture<SendResult<String, String>> send(
            UUID ticketId,
            String correlationId,
            String payload
    ) {
        String ticketID = ticketId.toString();

        return kafkaTemplate.send(
                KafkaTopics.TICKET_CREATED.getTopicName(),
                ticketID,
                payload
        );
    }*/

    public CompletableFuture<SendResult<String, String>> send(
            UUID ticketId,
            String correlationId,
            String payload
    ) {
        String ticketIdAsString = ticketId.toString();

        ProducerRecord<String, String> record =
                new ProducerRecord<>(
                        KafkaTopics.TICKET_CREATED.getTopicName(),
                        ticketIdAsString,
                        payload
                );

        record.headers().add(
                "correlationId",
                correlationId.getBytes(StandardCharsets.UTF_8)
        );

        return kafkaTemplate.send(record);
    }


}