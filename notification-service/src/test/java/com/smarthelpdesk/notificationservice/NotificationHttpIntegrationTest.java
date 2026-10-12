package com.smarthelpdesk.notificationservice;
import com.smarthelpdesk.notificationservice.support.DatabaseTestSupport;
import com.smarthelpdesk.notificationservice.mail.EmailCipher;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"notification.email.enabled=true","notification.email.from=helpdesk@example.test","notification.email.encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=","notification.email.poll-ms=3600000"})
@AutoConfigureMockMvc
class NotificationHttpIntegrationTest extends DatabaseTestSupport {
 @Autowired MockMvc mvc;@Autowired JdbcTemplate jdbc;@Value("${jwt.secret}") String secret;
 String token(UUID user){return io.jsonwebtoken.Jwts.builder().subject(user.toString()).claim("role","CUSTOMER").claim("type","access").claim("email","recipient@example.test").expiration(new Date(System.currentTimeMillis()+60000)).signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8))).compact();}
 @BeforeEach void clear(){jdbc.execute("TRUNCATE notification,email_subscription,email_outbox CASCADE");}
 @Test void ownHistoryReadAndReceiptAreIsolatedOverHttp() throws Exception {
  UUID owner=UUID.randomUUID(),id=UUID.randomUUID();String mine=token(owner),other=token(UUID.randomUUID());
  jdbc.update("INSERT INTO notification(id,user_id,ticket_id,type,content) VALUES (?,?,?,'TICKET_PROCESSED','ready')",id,owner,UUID.randomUUID());
  mvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/notifications").header("Authorization","Bearer "+other)).andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
  mvc.perform(get("/api/v1/notifications").header("Authorization","Bearer "+mine)).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(id.toString()));
  mvc.perform(post("/api/v1/notifications/"+id+"/delivered").header("Authorization","Bearer "+other)).andExpect(status().isNotFound());
  mvc.perform(patch("/api/v1/notifications/"+id+"/read").header("Authorization","Bearer "+other)).andExpect(status().isNotFound());
  mvc.perform(post("/api/v1/notifications/"+id+"/delivered").header("Authorization","Bearer "+mine)).andExpect(status().isNoContent());
  mvc.perform(patch("/api/v1/notifications/"+id+"/read").header("Authorization","Bearer "+mine)).andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/notifications?unreadOnly=true").header("Authorization","Bearer "+mine)).andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
 }
 @Test void emailSubscriptionConfirmationAndUnsubscribeThroughHttp() throws Exception {
  UUID user=UUID.randomUUID();String token=token(user);
  mvc.perform(post("/api/v1/notification-email/subscribe").header("Authorization","Bearer "+token)).andExpect(status().isAccepted());
  mvc.perform(post("/api/v1/notification-email/subscribe").header("Authorization","Bearer "+token)).andExpect(status().isTooManyRequests());
  UUID id=jdbc.queryForObject("SELECT id FROM email_outbox",UUID.class);String encrypted=jdbc.queryForObject("SELECT ciphertext FROM email_outbox",String.class);
  String text=new EmailCipher("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=").decrypt(encrypted,id.toString());
  text=tools.jackson.databind.json.JsonMapper.builder().build().readTree(text).get("text").asText();
  var matcher=java.util.regex.Pattern.compile("[A-Za-z0-9_-]{43}").matcher(text);org.assertj.core.api.Assertions.assertThat(matcher.find()).isTrue();
  String body="{\"token\":\""+matcher.group()+"\"}";
  mvc.perform(post("/api/v1/notification-email/confirm").header("Authorization","Bearer "+token(UUID.randomUUID())).contentType("application/json").content(body)).andExpect(status().isBadRequest());
  mvc.perform(post("/api/v1/notification-email/confirm").header("Authorization","Bearer "+token).contentType("application/json").content(body)).andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/notification-email").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true));
  mvc.perform(post("/api/v1/notification-email/retry-failed").header("Authorization","Bearer "+token)).andExpect(status().isAccepted());
  mvc.perform(delete("/api/v1/notification-email").header("Authorization","Bearer "+token)).andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/notification-email").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false));
 }
 @Test void invalidPaginationAndMissingCredentialsAreRejected() throws Exception {
  mvc.perform(get("/api/v1/notifications?size=101").header("Authorization","Bearer "+token(UUID.randomUUID()))).andExpect(status().isBadRequest());
  mvc.perform(post("/api/v1/notification-email/subscribe")).andExpect(status().isUnauthorized());
 }
}
