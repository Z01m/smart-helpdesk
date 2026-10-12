package com.smarthelpdesk.aiworker;
import com.smarthelpdesk.aiworker.ai.AiClient;
import com.smarthelpdesk.aiworker.client.KnowledgeBaseClient;
import com.smarthelpdesk.aiworker.dto.ai.AiResponse;
import com.smarthelpdesk.aiworker.dto.knowledge.KnowledgeSearchResponse;
import kafka.event.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import java.time.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest
@org.springframework.test.annotation.DirtiesContext
@EmbeddedKafka(partitions=1,topics={"ticket.created","ticket.processed","ticket.dlq"},bootstrapServersProperty="spring.kafka.bootstrap-servers")
class KafkaFlowIntegrationTest {
 static final PostgreSQLContainer<?> db=new PostgreSQLContainer<>("postgres:16-alpine");
 static {db.start();}
 @DynamicPropertySource static void config(DynamicPropertyRegistry r){r.add("spring.datasource.url",db::getJdbcUrl);r.add("spring.datasource.username",db::getUsername);r.add("spring.datasource.password",db::getPassword);r.add("ai.outbox.poll-ms",()->100);}
 @Autowired KafkaTemplate<String,String> kafka; @Autowired ObjectMapper mapper; @Autowired JdbcTemplate jdbc;
 @Autowired org.springframework.kafka.test.EmbeddedKafkaBroker broker;
 @MockitoBean AiClient ai; @MockitoBean KnowledgeBaseClient knowledge;
 @Test void realBrokerDeliversFallbackAndDeduplicatesInput() throws Exception {
  when(ai.chatCompletion(any())).thenReturn(new AiResponse("{\"category\":\"PAYMENT\",\"confidence\":0.9}",Map.of()),new AiResponse("{\"sentiment\":\"NEUTRAL\",\"score\":0}",Map.of()));
  when(ai.embed(any())).thenReturn(new float[]{1,0});when(knowledge.search(any())).thenReturn(new KnowledgeSearchResponse(List.of()));
  UUID ticket=UUID.randomUUID(),id=UUID.randomUUID();
  var payload=TicketCreatedEvent.builder().ticketId(ticket).userId(UUID.randomUUID()).message("Payment question").customerTier("STANDARD").createdAt(Instant.now()).build();
  var event=EventEnvelope.<TicketCreatedEvent>builder().eventId(id).eventType("TICKET_CREATED").occurredAt(Instant.now()).payload(payload).build();
  String json=mapper.writeValueAsString(event);kafka.send("ticket.created",ticket.toString(),json).get();kafka.send("ticket.created",ticket.toString(),json).get();
  org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).until(()->jdbc.queryForObject("SELECT count(*) FROM result_outbox WHERE id=? AND published_at IS NOT NULL",Long.class,id)==1);
  try(var consumer=consumer("ticket.processed")) {
   var records=org.springframework.kafka.test.utils.KafkaTestUtils.getRecords(consumer,Duration.ofSeconds(10),1);
   assertThat(records.count()).isGreaterThan(0);
   var result=mapper.readTree(records.iterator().next().value());assertThat(result.get("payload").get("requiresHumanReview").asBoolean()).isTrue();
   assertThat(result.get("payload").get("ticketId").asText()).isEqualTo(ticket.toString());
  }
  assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_results WHERE ticket_id=?",Long.class,ticket)).isEqualTo(1);
 }
 @Test void malformedEventGoesToRealDeadLetterTopic() throws Exception {
  kafka.send("ticket.created","poison","not-json").get();
  try(var consumer=consumer("ticket.dlq")) {
   var records=org.springframework.kafka.test.utils.KafkaTestUtils.getRecords(consumer,Duration.ofSeconds(30),1);
   assertThat(records.count()).isGreaterThan(0);assertThat(records.iterator().next().value()).isEqualTo("not-json");
  }
 }
 org.apache.kafka.clients.consumer.KafkaConsumer<String,String> consumer(String topic){
  var props=new HashMap<String,Object>();props.put("bootstrap.servers",broker.getBrokersAsString());props.put("group.id",UUID.randomUUID().toString());props.put("auto.offset.reset","earliest");props.put("key.deserializer","org.apache.kafka.common.serialization.StringDeserializer");props.put("value.deserializer","org.apache.kafka.common.serialization.StringDeserializer");
  var c=new org.apache.kafka.clients.consumer.KafkaConsumer<String,String>(props);c.subscribe(List.of(topic));return c;
 }
}
