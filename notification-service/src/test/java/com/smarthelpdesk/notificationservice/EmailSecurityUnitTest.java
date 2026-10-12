package com.smarthelpdesk.notificationservice;
import com.smarthelpdesk.notificationservice.mail.EmailCipher;
import com.smarthelpdesk.notificationservice.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailSecurityUnitTest {
    @Test void queuedSecretsAreEncryptedAndBoundToTheirRecord() {
        byte[] key = new byte[32]; new java.security.SecureRandom().nextBytes(key);
        var cipher = new EmailCipher(Base64.getEncoder().encodeToString(key));
        var value = cipher.encrypt("recipient@example.test and confirmation token", "record-1");
        assertThat(value).doesNotContain("recipient", "confirmation");
        assertThat(cipher.decrypt(value,"record-1")).contains("confirmation token");
        assertThatThrownBy(() -> cipher.decrypt(value,"record-2")).isInstanceOf(IllegalStateException.class);
    }
    @Test void expiredSocketCannotReceiveProtectedMessages() throws Exception {
        var guard = new WebSocketSessionGuard();
        var socket = mock(WebSocketSession.class);
        when(socket.getId()).thenReturn("expired");when(socket.isOpen()).thenReturn(true);
        guard.connected(socket);
        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID().toString(),null,List.of());
        authentication.setDetails(new AccessTokenDetails(Instant.now().minusSeconds(1),"user@example.test"));
        assertThatThrownBy(() -> guard.authenticate("expired",authentication)).isInstanceOf(IllegalArgumentException.class);
        assertThat(guard.authorized("expired")).isFalse();
    }
    @Test void longLivedSocketClosesAtTokenExpiry() throws Exception {
        var guard = new WebSocketSessionGuard();
        var socket = mock(WebSocketSession.class);
        when(socket.getId()).thenReturn("short-lived");when(socket.isOpen()).thenReturn(true);
        guard.connected(socket);
        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID().toString(),null,List.of());
        authentication.setDetails(new AccessTokenDetails(Instant.now().plusMillis(150),"user@example.test"));
        guard.authenticate("short-lived",authentication);
        org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(2)).untilAsserted(() -> {
            guard.expire(); verify(socket).close(CloseStatus.POLICY_VIOLATION);
        });
        assertThat(guard.authorized("short-lived")).isFalse();
    }

    @Test void connectionLimitIsEnforcedPerUser() throws Exception {
        var guard=new WebSocketSessionGuard();String user=UUID.randomUUID().toString();
        var authentication=new UsernamePasswordAuthenticationToken(user,null,List.of());authentication.setDetails(new AccessTokenDetails(Instant.now().plusSeconds(60),"user@example.test"));
        for(int i=0;i<6;i++) {var socket=mock(WebSocketSession.class);String id="session-"+i;when(socket.getId()).thenReturn(id);when(socket.isOpen()).thenReturn(true);guard.connected(socket);
            if(i<5)guard.authenticate(id,authentication);else assertThatThrownBy(()->guard.authenticate(id,authentication)).isInstanceOf(IllegalArgumentException.class);}
        guard.disconnected("session-0");guard.authenticate("session-5",authentication);assertThat(guard.authorized("session-5")).isTrue();
    }
    @Test void wildcardAndInsecureRemoteOriginsAreRejected() {
        var verifier=new AccessTokenVerifier("test-only-secret-that-is-long-enough-for-HMAC-123456789");var guard=new WebSocketSessionGuard();
        for(String origin:List.of("*","https://*.example.test","http://example.test","http://localhost.evil.test"))
            assertThatThrownBy(()->new com.smarthelpdesk.notificationservice.config.WebSocketConfig(verifier,guard,new String[]{origin})).isInstanceOf(IllegalArgumentException.class);
        new com.smarthelpdesk.notificationservice.config.WebSocketConfig(verifier,guard,new String[]{"https://helpdesk.example.test","http://localhost:3000"});
    }
}
