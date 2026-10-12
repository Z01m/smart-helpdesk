package com.smarthelpdesk.apigateway;
import com.smarthelpdesk.apigateway.kafka.*;
import com.smarthelpdesk.apigateway.repository.OutboxEventRepository;
import com.smarthelpdesk.apigateway.entity.OutboxEvent;
import org.junit.jupiter.api.Test;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.support.SendResult;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class GatewayOutboxTest {
 @Test void failedSendIsRetainedAndSuccessfulRetryIsMarkedPublished(){
  var repository=mock(OutboxEventRepository.class);var producer=mock(TicketEventProducer.class);var event=new OutboxEvent();event.setId(UUID.randomUUID());event.setAggregateId(UUID.randomUUID());event.setEventType("TICKET_CREATED");event.setPayload("{}");event.setPublished(false);
  when(repository.findByPublishedFalse()).thenReturn(List.of(event));when(producer.send(any(),any(),any(),any())).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("offline")));
  var poller=new OutboxEventPoller(repository,producer);poller.pollAndPublish();assertThat(event.isPublished()).isFalse();verify(repository,never()).save(any());
  var result=new SendResult<String,String>(new org.apache.kafka.clients.producer.ProducerRecord<>("ticket.created","{}"),new RecordMetadata(new TopicPartition("ticket.created",0),0,0,0,0,0));
  when(producer.send(any(),any(),any(),any())).thenReturn(CompletableFuture.completedFuture(result));poller.pollAndPublish();assertThat(event.isPublished()).isTrue();assertThat(event.getPublishedAt()).isNotNull();verify(repository).save(event);
 }
}
