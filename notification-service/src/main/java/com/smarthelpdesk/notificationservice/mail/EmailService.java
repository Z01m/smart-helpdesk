package com.smarthelpdesk.notificationservice.mail;

import jakarta.mail.internet.InternetAddress;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
@ConditionalOnProperty(name="notification.email.enabled", havingValue="true")
public class EmailService {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final JavaMailSender sender;
    private final ObjectMapper mapper;
    private final EmailCipher cipher;
    private final String from;
    private final SecureRandom random = new SecureRandom();
    public EmailService(JdbcTemplate jdbc, TransactionTemplate transactions, JavaMailSender sender, ObjectMapper mapper,
            @Value("${notification.email.encryption-key}") String key, @Value("${notification.email.from}") String from) {
        this.jdbc=jdbc; this.transactions=transactions; this.sender=sender; this.mapper=mapper;
        cipher=new EmailCipher(key); this.from=validEmail(from);
    }
    public record Status(boolean enabled, boolean verified, long failedMessages) {}
    public record Payload(String recipient, String subject, String text) {}
    private record Claimed(UUID id, UUID user, String kind, String ciphertext, int attempts, UUID lease) {}

    public void subscribe(UUID user, String accountEmail) {
        String email = validEmail(accountEmail);
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        transactions.executeWithoutResult(tx -> {
            int count = jdbc.update("""
                INSERT INTO email_subscription(user_id, email_ciphertext, challenge_hash, challenge_expires_at)
                VALUES (?, ?, ?, now() + interval '15 minutes')
                ON CONFLICT(user_id) DO UPDATE SET email_ciphertext=EXCLUDED.email_ciphertext,
                  challenge_hash=EXCLUDED.challenge_hash, challenge_expires_at=EXCLUDED.challenge_expires_at,
                  requested_at=now(), enabled=FALSE, verified_at=NULL
                WHERE email_subscription.requested_at < now() - interval '10 minutes'
                """, user, cipher.encrypt(email, user.toString()), hash(token));
            if (count == 0) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Wait 10 minutes before requesting another confirmation");
            jdbc.update("UPDATE email_outbox SET status='CANCELLED', ciphertext=NULL WHERE user_id=? AND status IN ('PENDING','FAILED')", user);
            enqueue(user, null, "VERIFY", new Payload(email, "Подтверждение уведомлений Smart Helpdesk",
                    "Вы запросили Email-уведомления. Код действует 15 минут:\n" + token +
                    "\nЕсли вы не запрашивали подписку, проигнорируйте письмо. Ответы и содержимое обращений по Email не отправляются."));
        });
    }
    public void confirm(UUID user, String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid confirmation code");
        int count = jdbc.update("""
            UPDATE email_subscription SET enabled=TRUE, verified_at=now(), challenge_hash=NULL, challenge_expires_at=NULL
            WHERE user_id=? AND challenge_hash=? AND challenge_expires_at > now()
            """, user, hash(token));
        if (count == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired confirmation code");
    }
    public void unsubscribe(UUID user) {
        transactions.executeWithoutResult(tx -> {
            jdbc.update("UPDATE email_subscription SET enabled=FALSE, verified_at=NULL, challenge_hash=NULL WHERE user_id=?", user);
            jdbc.update("UPDATE email_outbox SET status='CANCELLED', ciphertext=NULL WHERE user_id=? AND status IN ('PENDING','FAILED')", user);
        });
    }
    public Status status(UUID user) {
        return jdbc.query("""
            SELECT enabled, verified_at, (SELECT count(*) FROM email_outbox WHERE user_id=? AND status='FAILED') AS failed
            FROM email_subscription WHERE user_id=?
            """, (rs,row) -> new Status(rs.getBoolean("enabled"), rs.getTimestamp("verified_at") != null, rs.getLong("failed")), user,user)
            .stream().findFirst().orElse(new Status(false,false,0));
    }
    public void retryFailed(UUID user) {
        jdbc.update("""
            UPDATE email_outbox SET status='PENDING', attempts=0, available_at=now(), last_error=NULL
            WHERE user_id=? AND status='FAILED' AND kind='NOTIFICATION'
            AND EXISTS (SELECT 1 FROM email_subscription WHERE user_id=? AND enabled)
            """, user,user);
    }
    private void enqueue(UUID user, UUID notification, String kind, Payload payload) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO email_outbox(id,user_id,notification_id,kind,ciphertext,expires_at)
            VALUES (?,?,?,?,?, CASE WHEN ?='VERIFY' THEN now()+interval '15 minutes' ELSE NULL END)
            ON CONFLICT(notification_id) DO NOTHING
            """,id,user,notification,kind,cipher.encrypt(mapper.writeValueAsString(payload),id.toString()),kind);
    }
    @Scheduled(fixedDelayString="${notification.email.poll-ms:5000}")
    public void dispatch() {
        // Email and WebSocket have independent delivery state, so either channel can fail independently.
        var pending = jdbc.query("""
            SELECT n.id,n.user_id,s.email_ciphertext FROM notification n JOIN email_subscription s ON s.user_id=n.user_id
            WHERE s.enabled AND s.verified_at IS NOT NULL AND n.created_at >= s.verified_at
              AND NOT EXISTS(SELECT 1 FROM email_outbox e WHERE e.notification_id=n.id)
            ORDER BY n.created_at LIMIT 100
            """,(rs,row)->Map.entry(rs.getObject("id",UUID.class), Map.entry(rs.getObject("user_id",UUID.class),rs.getString("email_ciphertext"))));
        for (var item:pending) {
            UUID user=item.getValue().getKey();
            enqueue(user,item.getKey(),"NOTIFICATION",new Payload(cipher.decrypt(item.getValue().getValue(),user.toString()),
                    "Новое уведомление Smart Helpdesk", "В Smart Helpdesk появилось новое уведомление. Откройте приложение, чтобы прочитать его."));
        }
        jdbc.update("UPDATE email_outbox SET status='CANCELLED',ciphertext=NULL WHERE expires_at <= now() AND status IN ('PENDING','SENDING','FAILED')");
        for (int i=0;i<20;i++) {
            UUID lease=UUID.randomUUID();
            Claimed claimed=transactions.execute(tx -> jdbc.query("""
                UPDATE email_outbox e SET status='SENDING',lease_id=?,lease_until=now()+interval '2 minutes',attempts=e.attempts+1
                WHERE e.id=(SELECT id FROM email_outbox WHERE
                    ((status='PENDING' AND available_at<=now()) OR (status='SENDING' AND lease_until<now()))
                    AND (expires_at IS NULL OR expires_at>now()) ORDER BY available_at LIMIT 1 FOR UPDATE SKIP LOCKED)
                RETURNING id,user_id,kind,ciphertext,attempts
                """,(rs,row)->new Claimed(rs.getObject("id",UUID.class),rs.getObject("user_id",UUID.class),
                    rs.getString("kind"),rs.getString("ciphertext"),rs.getInt("attempts"),lease),lease).stream().findFirst().orElse(null));
            if (claimed == null) break;
            try {
                if (!eligible(claimed)) {
                    finish(claimed,"CANCELLED",null); continue;
                }
                Payload payload=mapper.readValue(cipher.decrypt(claimed.ciphertext(),claimed.id().toString()),Payload.class);
                var message=sender.createMimeMessage();
                var helper=new MimeMessageHelper(message,StandardCharsets.UTF_8.name());
                helper.setFrom(from);helper.setTo(validEmail(payload.recipient()));helper.setSubject(payload.subject());
                helper.setText(payload.text(),false); // Deliberately plain text; ticket/LLM content is never interpreted as HTML.
                message.setHeader("X-Smart-Helpdesk-Delivery",claimed.id().toString());
                sender.send(message);
                finish(claimed,"SENT",null);
            } catch (Exception ex) {
                int delay=Math.min(3600, 30 * (1 << Math.min(claimed.attempts(),6)));
                jdbc.update("""
                    UPDATE email_outbox SET status=?,available_at=now()+(? * interval '1 second'),last_error=?,lease_id=NULL,lease_until=NULL
                    WHERE id=? AND lease_id=?
                    """, claimed.attempts()>=8 ? "FAILED":"PENDING",delay,ex.getClass().getSimpleName(),claimed.id(),claimed.lease());
            }
        }
    }
    private boolean eligible(Claimed c) {
        String condition=c.kind().equals("VERIFY") ? "challenge_hash IS NOT NULL AND challenge_expires_at>now()" : "enabled AND verified_at IS NOT NULL";
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM email_subscription WHERE user_id=? AND "+condition+")",Boolean.class,c.user()));
    }
    private void finish(Claimed c,String status,String error) {
        jdbc.update("UPDATE email_outbox SET status=?,ciphertext=NULL,lease_id=NULL,lease_until=NULL,last_error=? WHERE id=? AND lease_id=?",
                status,error,c.id(),c.lease());
    }
    private static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    private static String validEmail(String value) {
        try {
            if(value==null || value.length()>254 || value.contains("\r") || value.contains("\n")) throw new IllegalArgumentException();
            var address=new InternetAddress(value,true);address.validate();
            if(!value.equals(address.getAddress()))throw new IllegalArgumentException();
            return value;
        } catch(Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A valid account email is required"); }
    }
}
