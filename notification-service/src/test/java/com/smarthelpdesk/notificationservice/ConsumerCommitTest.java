package com.smarthelpdesk.notificationservice;
import com.smarthelpdesk.notificationservice.kafka.*;
import com.smarthelpdesk.notificationservice.service.NotificationService;
import kafka.event.*;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;
import tools.jackson.databind.json.JsonMapper;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class ConsumerCommitTest {
 final NotificationService service=mock(NotificationService.class);
 final Acknowledgment ack=mock(Acknowledgment.class);
 final tools.jackson.databind.ObjectMapper mapper=JsonMapper.builder().build();
 @Test void processedEventAcknowledgesOnlyAfterServiceReturns(){
  UUID id=UUID.randomUUID();var payload=TicketProcessedEvent.builder().ticketId(UUID.randomUUID()).build();
  String json=mapper.writeValueAsString(EventEnvelope.<TicketProcessedEvent>builder().eventId(id).payload(payload).build());
  new TicketProcessedConsumer(mapper,service).consume(json,ack);
  var order=inOrder(service,ack);order.verify(service).process(id,payload);order.verify(ack).acknowledge();
 }
 @Test void failedTransactionCannotAcknowledgeOffset(){
  doThrow(new IllegalStateException("database unavailable")).when(service).process(any(),any());
  String json=mapper.writeValueAsString(EventEnvelope.<TicketProcessedEvent>builder().eventId(UUID.randomUUID()).payload(TicketProcessedEvent.builder().ticketId(UUID.randomUUID()).build()).build());
  assertThatThrownBy(()->new TicketProcessedConsumer(mapper,service).consume(json,ack)).isInstanceOf(IllegalStateException.class);verifyNoInteractions(ack);
 }
 @Test void statusEventHasSameCommitOrdering(){
  UUID id=UUID.randomUUID();var payload=new TicketStatusChangedEvent(UUID.randomUUID(),"ANSWED","CLOSED",UUID.randomUUID(),java.time.Instant.now());
  String json=mapper.writeValueAsString(EventEnvelope.<TicketStatusChangedEvent>builder().eventId(id).payload(payload).build());
  new TicketStatusChangedConsumer(mapper,service).consume(json,ack);
  var order=inOrder(service,ack);order.verify(service).processStatusChange(id,payload);order.verify(ack).acknowledge();
 }
 @Test void malformedInputCannotAcknowledgeOrTouchService(){assertThatThrownBy(()->new TicketProcessedConsumer(mapper,service).consume("not json",ack)).isInstanceOf(RuntimeException.class);verifyNoInteractions(ack,service);}
}
