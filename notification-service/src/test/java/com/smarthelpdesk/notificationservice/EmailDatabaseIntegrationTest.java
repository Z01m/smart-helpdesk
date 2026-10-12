package com.smarthelpdesk.notificationservice;
import com.smarthelpdesk.notificationservice.mail.EmailService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class EmailDatabaseIntegrationTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    JdbcTemplate jdbc;TransactionTemplate transactions;String key;
    @BeforeEach void setup() {
        var source = new DriverManagerDataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword());
        Flyway.configure().dataSource(source).load().migrate();jdbc=new JdbcTemplate(source);
        jdbc.execute("TRUNCATE notification,email_subscription,email_outbox CASCADE");
        transactions=new TransactionTemplate(new DataSourceTransactionManager(source));
        byte[] bytes=new byte[32];new java.security.SecureRandom().nextBytes(bytes);key=Base64.getEncoder().encodeToString(bytes);
    }
    EmailService service(int port) {
        var sender=new JavaMailSenderImpl();sender.setHost("127.0.0.1");sender.setPort(port);
        sender.getJavaMailProperties().setProperty("mail.smtp.connectiontimeout","1000");
        sender.getJavaMailProperties().setProperty("mail.smtp.timeout","1000");
        return new EmailService(jdbc,transactions,sender,JsonMapper.builder().build(),key,"helpdesk@example.test");
    }
    @Test void confirmationAndNotificationTravelThroughRealLocalSmtp() throws Exception {
        try(var smtp=new LocalSmtpServer()) {
            var service=service(smtp.port());UUID user=UUID.randomUUID();
            service.subscribe(user,"recipient@example.test");
            assertThat(service.status(user).enabled()).isFalse();
            assertThatThrownBy(() -> service.subscribe(user,"recipient@example.test")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
            assertThat(jdbc.queryForObject("SELECT ciphertext FROM email_outbox",String.class)).doesNotContain("recipient@example.test");
            service.dispatch();
            String raw=smtp.received.poll(5,java.util.concurrent.TimeUnit.SECONDS);assertThat(raw).isNotNull();
            var message=new jakarta.mail.internet.MimeMessage(jakarta.mail.Session.getInstance(new Properties()),
                    new java.io.ByteArrayInputStream(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            var matcher=java.util.regex.Pattern.compile("[A-Za-z0-9_-]{43}").matcher(message.getContent().toString());
            assertThat(matcher.find()).isTrue();String token=matcher.group();
            assertThatThrownBy(() -> service.confirm(UUID.randomUUID(),token)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
            service.confirm(user,token);assertThat(service.status(user).enabled()).isTrue();
            assertThatThrownBy(() -> service.confirm(user,token)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
            jdbc.update("INSERT INTO notification(id,user_id,ticket_id,type,content) VALUES (?,?,?,'TICKET_PROCESSED','private ticket details')",
                    UUID.randomUUID(),user,UUID.randomUUID());
            service.dispatch();String alert=smtp.received.poll(5,java.util.concurrent.TimeUnit.SECONDS);
            assertThat(alert).isNotNull().doesNotContain("private ticket details");
            service.dispatch();assertThat(smtp.received).isEmpty();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM email_outbox WHERE ciphertext IS NOT NULL",Long.class)).isZero();
            service.unsubscribe(user);assertThat(service.status(user).enabled()).isFalse();
        }
    }
    @Test void smtpFailureKeepsEncryptedMailForRetry() throws Exception {
        int closedPort;
        try(var socket=new java.net.ServerSocket(0)){closedPort=socket.getLocalPort();}
        var service=service(closedPort);service.subscribe(UUID.randomUUID(),"recipient@example.test");service.dispatch();
        assertThat(jdbc.queryForObject("SELECT status FROM email_outbox",String.class)).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT attempts FROM email_outbox",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT ciphertext FROM email_outbox",String.class)).isNotBlank();
    }

    @Test void expiredChallengeCannotEnableDelivery() throws Exception {
        try(var smtp=new LocalSmtpServer()) {
            var service=service(smtp.port());UUID user=UUID.randomUUID();service.subscribe(user,"recipient@example.test");
            jdbc.update("UPDATE email_subscription SET challenge_expires_at=now()-interval '1 second'");
            assertThatThrownBy(()->service.confirm(user,"a".repeat(43))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
            service.dispatch();assertThat(smtp.received).isEmpty();
            assertThat(jdbc.queryForObject("SELECT status FROM email_outbox",String.class)).isEqualTo("CANCELLED");
        }
    }
    @Test void unsubscribeCancelsUnsentConfirmationAndErasesPayload() throws Exception {
        try(var smtp=new LocalSmtpServer()) {
            var service=service(smtp.port());UUID user=UUID.randomUUID();service.subscribe(user,"recipient@example.test");service.unsubscribe(user);service.dispatch();
            assertThat(smtp.received).isEmpty();assertThat(jdbc.queryForObject("SELECT ciphertext FROM email_outbox",String.class)).isNull();
        }
    }
    @Test void concurrentDispatchersSendOneLeasedMessage() throws Exception {
        try(var smtp=new LocalSmtpServer();var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first=service(smtp.port());var second=service(smtp.port());first.subscribe(UUID.randomUUID(),"recipient@example.test");
            var gate=new java.util.concurrent.CountDownLatch(1);
            var a=pool.submit(()->{gate.await();first.dispatch();return true;});var b=pool.submit(()->{gate.await();second.dispatch();return true;});gate.countDown();
            a.get(10,java.util.concurrent.TimeUnit.SECONDS);b.get(10,java.util.concurrent.TimeUnit.SECONDS);
            assertThat(smtp.received).hasSize(1);assertThat(jdbc.queryForObject("SELECT status FROM email_outbox",String.class)).isEqualTo("SENT");
        }
    }
    @Test void abandonedLeaseIsRecovered() throws Exception {
        try(var smtp=new LocalSmtpServer()) {
            var service=service(smtp.port());service.subscribe(UUID.randomUUID(),"recipient@example.test");
            jdbc.update("UPDATE email_outbox SET status='SENDING',lease_id=?,lease_until=now()-interval '1 second'",UUID.randomUUID());
            service.dispatch();assertThat(smtp.received).hasSize(1);
        }
    }
    @Test void rejectsHeaderInjectionBeforeQueueing() {
        var service=service(1);assertThatThrownBy(()->service.subscribe(UUID.randomUUID(),"victim@example.test\r\nBcc:other@example.test")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM email_outbox",Long.class)).isZero();
    }


    @Test void requiredTlsNeverFallsBackToPlaintextMail() throws Exception {
        try(var smtp=new LocalSmtpServer()) {
            var sender=new JavaMailSenderImpl();sender.setHost("127.0.0.1");sender.setPort(smtp.port());
            sender.getJavaMailProperties().setProperty("mail.smtp.starttls.enable","true");sender.getJavaMailProperties().setProperty("mail.smtp.starttls.required","true");sender.getJavaMailProperties().setProperty("mail.smtp.timeout","1000");
            var service=new EmailService(jdbc,transactions,sender,JsonMapper.builder().build(),key,"helpdesk@example.test");
            service.subscribe(UUID.randomUUID(),"recipient@example.test");service.dispatch();
            assertThat(smtp.received).isEmpty();assertThat(jdbc.queryForObject("SELECT status FROM email_outbox",String.class)).isEqualTo("PENDING");
        }
    }
    @Test void failedNotificationCanBeExplicitlyRetriedAndThenScrubbed() throws Exception {
        UUID user=UUID.randomUUID();int closedPort;try(var socket=new java.net.ServerSocket(0)){closedPort=socket.getLocalPort();}
        var offline=service(closedPort);offline.subscribe(user,"recipient@example.test");
        jdbc.update("UPDATE email_subscription SET enabled=TRUE,verified_at=now()-interval '1 minute',challenge_hash=NULL");
        jdbc.update("UPDATE email_outbox SET status='CANCELLED',ciphertext=NULL");
        jdbc.update("INSERT INTO notification(id,user_id,ticket_id,type,content) VALUES (?,?,?,'TICKET_PROCESSED','private')",UUID.randomUUID(),user,UUID.randomUUID());
        offline.dispatch();jdbc.update("UPDATE email_outbox SET attempts=7,available_at=now() WHERE kind='NOTIFICATION'");offline.dispatch();
        assertThat(offline.status(user).failedMessages()).isEqualTo(1);offline.retryFailed(user);
        try(var smtp=new LocalSmtpServer()) {service(smtp.port()).dispatch();assertThat(smtp.received).hasSize(1);}
        assertThat(offline.status(user).failedMessages()).isZero();assertThat(jdbc.queryForObject("SELECT ciphertext FROM email_outbox WHERE kind='NOTIFICATION'",String.class)).isNull();
    }
}
