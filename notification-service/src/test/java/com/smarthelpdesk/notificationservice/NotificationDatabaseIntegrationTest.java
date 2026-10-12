package com.smarthelpdesk.notificationservice;
import com.smarthelpdesk.notificationservice.client.GatewayClient;
import com.smarthelpdesk.notificationservice.dto.gateway.TicketOwnerResponse;
import com.smarthelpdesk.notificationservice.service.NotificationService;
import kafka.event.TicketProcessedEvent;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.kafka.listener.auto-startup=false", "notification.delivery-delay-ms=3600000"})
class NotificationDatabaseIntegrationTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @Autowired NotificationService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired com.smarthelpdesk.notificationservice.service.NotificationDeliveryJob delivery;
    @Autowired org.springframework.messaging.simp.user.SimpUserRegistry users;
    @org.springframework.beans.factory.annotation.Value("${local.server.port}") int port;
    @org.springframework.beans.factory.annotation.Value("${jwt.secret}") String jwtSecret;
    @MockitoBean GatewayClient gateway;
    @BeforeEach void clear() { jdbc.execute("TRUNCATE notification, processed_event CASCADE"); }
    @Test void redeliveryCreatesExactlyOneNotification() {
        UUID id = UUID.randomUUID(), ticket = UUID.randomUUID();
        when(gateway.getTicketOwner(ticket)).thenReturn(new TicketOwnerResponse(ticket, UUID.randomUUID()));
        var payload = TicketProcessedEvent.builder().ticketId(ticket).build();
        service.process(id, payload); service.process(id, payload);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notification", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM processed_event", Long.class)).isEqualTo(1);
        verify(gateway, times(1)).getTicketOwner(ticket);
    }
    @Test void gatewayFailureRollsBackClaimAndAllowsRetry() {
        UUID id = UUID.randomUUID(), ticket = UUID.randomUUID();
        when(gateway.getTicketOwner(ticket)).thenThrow(new IllegalStateException("offline"))
                .thenReturn(new TicketOwnerResponse(ticket, UUID.randomUUID()));
        var payload = TicketProcessedEvent.builder().ticketId(ticket).build();
        assertThatThrownBy(() -> service.process(id, payload)).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM processed_event", Long.class)).isZero();
        service.process(id, payload);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notification", Long.class)).isEqualTo(1);
    }

    @Test void authenticatedWebSocketReceivesCommittedNotification() throws Exception {
        UUID user = UUID.randomUUID(), ticket = UUID.randomUUID();
        String token = io.jsonwebtoken.Jwts.builder().subject(user.toString()).claim("type", "access")
                .claim("role", "CUSTOMER").expiration(new java.util.Date(System.currentTimeMillis() + 60000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8))).compact();
        var client = new org.springframework.web.socket.messaging.WebSocketStompClient(
                new org.springframework.web.socket.client.standard.StandardWebSocketClient());
        client.setMessageConverter(new org.springframework.messaging.converter.AbstractMessageConverter(
                org.springframework.util.MimeTypeUtils.APPLICATION_JSON) {
            @Override protected boolean supports(Class<?> type) { return type == String.class; }
            @Override protected Object convertFromInternal(org.springframework.messaging.Message<?> message,
                    Class<?> targetClass, Object hint) {
                return new String((byte[]) message.getPayload(), java.nio.charset.StandardCharsets.UTF_8);
            }
        });
        var received = new java.util.concurrent.LinkedBlockingQueue<String>();
        org.springframework.messaging.simp.stomp.StompSession session = null;
        try {
            var connectHeaders = new org.springframework.messaging.simp.stomp.StompHeaders();
            connectHeaders.set("Authorization", "Bearer " + token);
            session = client.connectAsync("ws://localhost:" + port + "/ws/notifications",
                    new org.springframework.web.socket.WebSocketHttpHeaders(), connectHeaders,
                    new org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter() {}).get(10, java.util.concurrent.TimeUnit.SECONDS);
            session.subscribe("/user/queue/notifications", new org.springframework.messaging.simp.stomp.StompFrameHandler() {
                @Override public java.lang.reflect.Type getPayloadType(org.springframework.messaging.simp.stomp.StompHeaders headers) { return String.class; }
                @Override public void handleFrame(org.springframework.messaging.simp.stomp.StompHeaders headers, Object payload) { received.add((String) payload); }
            });
            org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(5)).until(() ->
                    users.getUser(user.toString()) != null && users.getUser(user.toString()).getSessions().stream()
                            .anyMatch(s -> !s.getSubscriptions().isEmpty()));
            when(gateway.getTicketOwner(ticket)).thenReturn(new TicketOwnerResponse(ticket, user));
            service.process(UUID.randomUUID(), TicketProcessedEvent.builder().ticketId(ticket).build());
            delivery.deliver();
            String message = received.poll(10, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(message).isNotNull().contains(ticket.toString()).contains("TICKET_PROCESSED");
            assertThat(jdbc.queryForObject("SELECT ws_published FROM notification WHERE ticket_id = ?", Boolean.class, ticket)).isFalse();
            UUID notification = jdbc.queryForObject("SELECT id FROM notification WHERE ticket_id = ?", UUID.class, ticket);
            assertThatThrownBy(() -> service.acknowledge(UUID.randomUUID(), notification)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
            service.acknowledge(user, notification);
            assertThat(jdbc.queryForObject("SELECT delivered_at IS NOT NULL FROM notification WHERE id = ?", Boolean.class, notification)).isTrue();
        } finally {
            if (session != null && session.isConnected()) session.disconnect();
            client.stop();
        }
    }

    @Test void anotherInstanceDeliversAndReconnectReplaysUntilReceipt() throws Exception {
        UUID user=UUID.randomUUID(), ticket=UUID.randomUUID();
        String token=io.jsonwebtoken.Jwts.builder().subject(user.toString()).claim("type","access").claim("role","CUSTOMER")
                .expiration(new java.util.Date(System.currentTimeMillis()+120000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8))).compact();
        try (var second = new org.springframework.boot.builder.SpringApplicationBuilder(NotificationServiceApplication.class).run(
                "--server.port=0", "--spring.datasource.url="+postgres.getJdbcUrl(),
                "--spring.datasource.username="+postgres.getUsername(), "--spring.datasource.password="+postgres.getPassword(),
                "--spring.kafka.listener.auto-startup=false", "--notification.delivery-delay-ms=3600000", "--notification.email.enabled=false")) {
            int secondPort=Integer.parseInt(second.getEnvironment().getProperty("local.server.port"));
            var secondUsers=second.getBean(org.springframework.messaging.simp.user.SimpUserRegistry.class);
            UUID id;
            try (var connection=new TestConnection(secondPort,token)) {
                org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(5)).until(() ->
                        secondUsers.getUser(user.toString())!=null && secondUsers.getUser(user.toString()).getSessions().stream()
                                .anyMatch(s->!s.getSubscriptions().isEmpty()));
                when(gateway.getTicketOwner(ticket)).thenReturn(new TicketOwnerResponse(ticket,user));
                service.process(UUID.randomUUID(),TicketProcessedEvent.builder().ticketId(ticket).build());
                delivery.deliver(); // Event was consumed on the first instance; it has no local session for this user.
                second.getBean(com.smarthelpdesk.notificationservice.service.NotificationDeliveryJob.class).deliver();
                assertThat(connection.messages.poll(5,java.util.concurrent.TimeUnit.SECONDS)).contains(ticket.toString());
                id=jdbc.queryForObject("SELECT id FROM notification WHERE ticket_id=?",UUID.class,ticket);
                assertThat(jdbc.queryForObject("SELECT delivered_at IS NULL FROM notification WHERE id=?",Boolean.class,id)).isTrue();
            }
            try (var reconnected=new TestConnection(port,token)) {
                org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(5)).until(() ->
                        users.getUser(user.toString())!=null && users.getUser(user.toString()).getSessions().stream()
                                .anyMatch(s->!s.getSubscriptions().isEmpty()));
                delivery.deliver();
                assertThat(reconnected.messages.poll(5,java.util.concurrent.TimeUnit.SECONDS)).contains(id.toString());
                service.acknowledge(user,id);
            }
        }
    }

    @Test void credentialsInWebSocketUrlAreRejected() throws Exception {
        var response=java.net.http.HttpClient.newHttpClient().send(java.net.http.HttpRequest.newBuilder(
                java.net.URI.create("http://localhost:"+port+"/ws/notifications?token=secret")).GET().build(),
                java.net.http.HttpResponse.BodyHandlers.discarding());
        assertThat(response.statusCode()).isEqualTo(400);
    }

    private static final class TestConnection implements AutoCloseable {
        final java.util.concurrent.BlockingQueue<String> messages=new java.util.concurrent.LinkedBlockingQueue<>();
        final org.springframework.web.socket.messaging.WebSocketStompClient client;
        final org.springframework.messaging.simp.stomp.StompSession session;
        TestConnection(int port,String token) throws Exception {
            client=new org.springframework.web.socket.messaging.WebSocketStompClient(new org.springframework.web.socket.client.standard.StandardWebSocketClient());
            client.setMessageConverter(new org.springframework.messaging.converter.AbstractMessageConverter(org.springframework.util.MimeTypeUtils.APPLICATION_JSON) {
                @Override protected boolean supports(Class<?> type){return type==String.class;}
                @Override protected Object convertFromInternal(org.springframework.messaging.Message<?> m,Class<?> type,Object hint){
                    return new String((byte[])m.getPayload(),java.nio.charset.StandardCharsets.UTF_8);
                }
            });
            var headers=new org.springframework.messaging.simp.stomp.StompHeaders();headers.set("Authorization","Bearer "+token);
            session=client.connectAsync("ws://localhost:"+port+"/ws/notifications",new org.springframework.web.socket.WebSocketHttpHeaders(),headers,
                    new org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter(){}).get(10,java.util.concurrent.TimeUnit.SECONDS);
            session.subscribe("/user/queue/notifications",new org.springframework.messaging.simp.stomp.StompFrameHandler(){
                @Override public java.lang.reflect.Type getPayloadType(org.springframework.messaging.simp.stomp.StompHeaders h){return String.class;}
                @Override public void handleFrame(org.springframework.messaging.simp.stomp.StompHeaders h,Object body){messages.add((String)body);}
            });
        }
        @Override public void close(){if(session.isConnected())session.disconnect();client.stop();}
    }

    @Test void concurrentDuplicateEventsCreateOneNotification() throws Exception {
        UUID event=UUID.randomUUID(),ticket=UUID.randomUUID(),user=UUID.randomUUID();
        when(gateway.getTicketOwner(ticket)).thenReturn(new TicketOwnerResponse(ticket,user));
        var payload=TicketProcessedEvent.builder().ticketId(ticket).build();var gate=new java.util.concurrent.CountDownLatch(1);
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(6)) {
            var results=new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for(int i=0;i<6;i++)results.add(pool.submit(()->{gate.await();service.process(event,payload);return true;}));
            gate.countDown();for(var result:results)result.get(10,java.util.concurrent.TimeUnit.SECONDS);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notification",Long.class)).isEqualTo(1);
        verify(gateway,times(1)).getTicketOwner(ticket);
    }
    @Test void readingAndAcknowledgingCannotCrossUserBoundary() {
        UUID owner=UUID.randomUUID(),ticket=UUID.randomUUID(),other=UUID.randomUUID();
        when(gateway.getTicketOwner(ticket)).thenReturn(new TicketOwnerResponse(ticket,owner));
        service.process(UUID.randomUUID(),TicketProcessedEvent.builder().ticketId(ticket).requiresHumanReview(true).build());
        var item=service.list(owner,true,0,10).getContent().getFirst();
        assertThat(service.list(other,false,0,10)).isEmpty();
        assertThatThrownBy(()->service.markRead(other,item.id())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(()->service.acknowledge(other,item.id())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        service.acknowledge(owner,item.id());service.acknowledge(owner,item.id());service.markRead(owner,item.id());service.markRead(owner,item.id());
        assertThat(service.list(owner,true,0,10)).isEmpty();assertThat(service.list(owner,false,0,10)).hasSize(1);
    }
    @Test void statusEventIsPersistedAndDeduplicated() {
        UUID owner=UUID.randomUUID(),ticket=UUID.randomUUID(),event=UUID.randomUUID();
        when(gateway.getTicketOwner(ticket)).thenReturn(new TicketOwnerResponse(ticket,owner));
        var payload=new kafka.event.TicketStatusChangedEvent(ticket,"ANSWED","CLOSED",owner,java.time.Instant.now());
        service.processStatusChange(event,payload);service.processStatusChange(event,payload);
        assertThat(service.list(owner,false,0,10).getContent()).singleElement().satisfies(n->assertThat(n.content()).contains("CLOSED"));
    }

}
