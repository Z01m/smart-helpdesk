package com.smarthelpdesk.apigateway.kafka;

import com.smarthelpdesk.apigateway.entity.OutboxEvent;
import com.smarthelpdesk.apigateway.repository.OutboxEventRepository;
import org.springframework.kafka.support.SendResult;
import kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPoller {

    private final OutboxEventRepository outboxEventRepository;

    private final TicketEventProducer ticketEventProducer;

    @Scheduled(fixedDelay = 60000)
    public void pollAndPublish(){
        List<OutboxEvent> outboxEvents = outboxEventRepository.findByPublishedFalse();
        log.info("Found {} unpublished outbox events", outboxEvents.size());

        for (OutboxEvent outboxEvent : outboxEvents) {
            if(!KafkaTopics.TICKET_CREATED.name().equals(outboxEvent.getEventType()))
            {
                log.warn("Unsupported outbox event type: {}", outboxEvent.getEventType());
                continue;
            }

            CompletableFuture<SendResult<String,String>> future = ticketEventProducer.send(
                    outboxEvent.getAggregateId(),
                    outboxEvent.getCorrelationId(),
                    outboxEvent.getPayload());

            future.whenComplete((result, exception) -> {

                if (exception != null) {

                    log.error(
                            "Failed to publish outbox event {}",
                            outboxEvent.getId(),
                            exception
                    );

                    return;
                }
                outboxEvent.setPublished(true);
                outboxEvent.setPublishedAt(Instant.now());

                outboxEventRepository.save(outboxEvent);

                log.info(
                        "Outbox event {} successfully published to topic {}, partition {}, offset {}",
                        outboxEvent.getId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset()
                );
            });
        }

    }
}
