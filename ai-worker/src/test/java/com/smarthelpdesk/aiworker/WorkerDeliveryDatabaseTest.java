package com.smarthelpdesk.aiworker;
import com.smarthelpdesk.aiworker.support.DatabaseTestSupport;
import com.smarthelpdesk.aiworker.service.*;
import com.smarthelpdesk.aiworker.rag.RagPipeline;
import com.smarthelpdesk.aiworker.producer.ResultOutboxPublisher;
import com.smarthelpdesk.aiworker.dto.ai.*;
import com.smarthelpdesk.aiworker.dto.ai.enums.*;
import kafka.event.TicketCreatedEvent;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest
class WorkerDeliveryDatabaseTest extends DatabaseTestSupport {
 @Autowired TicketEventProcessingService processing; @Autowired ResultOutboxPublisher publisher; @Autowired JdbcTemplate jdbc;
 @MockitoBean ClassificationService classification; @MockitoBean SentimentAnalysisService sentiment; @MockitoBean RagPipeline rag;
 @MockitoBean KafkaTemplate<String,String> kafka;
 @BeforeEach void setup(){jdbc.execute("TRUNCATE processed_event,ticket_results,result_outbox");when(classification.classify(any())).thenReturn(new ClassificationResult(TicketCategory.PAYMENT,.9));when(sentiment.analyze(any())).thenReturn(new SentimentResult(SentimentType.NEUTRAL,0));when(rag.retrieveContext(anyString())).thenReturn(List.of());}
 TicketCreatedEvent event(){return TicketCreatedEvent.builder().ticketId(UUID.randomUUID()).userId(UUID.randomUUID()).message("Payment question").customerTier("STANDARD").createdAt(java.time.Instant.now()).build();}
 long count(String table){return jdbc.queryForObject("SELECT count(*) FROM "+table,Long.class);}
 @Test void kafkaFailureKeepsStableOutputAndDoesNotRecomputeAnswer(){
  var event=event();UUID id=UUID.randomUUID();processing.process(id,event,"test");String payload=jdbc.queryForObject("SELECT payload FROM result_outbox",String.class);
  when(kafka.send(org.mockito.ArgumentMatchers.<org.apache.kafka.clients.producer.ProducerRecord<String,String>>any())).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("offline")));
  publisher.publish();processing.process(id,event,"test");
  assertThat(count("ticket_results")).isEqualTo(1);assertThat(jdbc.queryForObject("SELECT published_at FROM result_outbox",java.sql.Timestamp.class)).isNull();
  assertThat(jdbc.queryForObject("SELECT payload FROM result_outbox",String.class)).isEqualTo(payload);
  when(kafka.send(org.mockito.ArgumentMatchers.<org.apache.kafka.clients.producer.ProducerRecord<String,String>>any())).thenReturn(CompletableFuture.completedFuture(null));
  publisher.publish();assertThat(jdbc.queryForObject("SELECT published_at FROM result_outbox",java.sql.Timestamp.class)).isNotNull();
  verify(classification,times(1)).classify(any());
 }
 @Test void aiFailureRollsBackClaimAndAllowsRetry(){
  var event=event();UUID id=UUID.randomUUID();when(classification.classify(any())).thenThrow(new IllegalStateException("offline")).thenReturn(new ClassificationResult(TicketCategory.PAYMENT,.9));
  assertThatThrownBy(()->processing.process(id,event,null)).isInstanceOf(IllegalStateException.class);
  assertThat(count("processed_event")).isZero();assertThat(count("result_outbox")).isZero();
  processing.process(id,event,null);assertThat(count("ticket_results")).isEqualTo(1);assertThat(count("result_outbox")).isEqualTo(1);
 }
 @Test void concurrentRedeliveryCreatesOneResultAndOneOutputEvent() throws Exception {
  var event=event();UUID id=UUID.randomUUID();var gate=new CountDownLatch(1);
  try(var pool=Executors.newFixedThreadPool(4)){var results=new ArrayList<Future<?>>();for(int i=0;i<4;i++)results.add(pool.submit(()->{gate.await();processing.process(id,event,null);return true;}));gate.countDown();for(var f:results)f.get(15,TimeUnit.SECONDS);}
  assertThat(count("processed_event")).isEqualTo(1);assertThat(count("ticket_results")).isEqualTo(1);assertThat(count("result_outbox")).isEqualTo(1);
 }
}
