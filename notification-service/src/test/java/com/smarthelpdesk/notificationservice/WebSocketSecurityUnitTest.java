package com.smarthelpdesk.notificationservice;
import com.smarthelpdesk.notificationservice.config.StompAuthenticationInterceptor;
import com.smarthelpdesk.notificationservice.security.AccessTokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class WebSocketSecurityUnitTest {
    private final com.smarthelpdesk.notificationservice.security.WebSocketSessionGuard sessions = mock(com.smarthelpdesk.notificationservice.security.WebSocketSessionGuard.class);
    private final StompAuthenticationInterceptor interceptor = new StompAuthenticationInterceptor(
            new AccessTokenVerifier("01234567890123456789012345678901234567890123456789"), sessions);
    private Message<byte[]> message(StompCommand command, String destination, boolean authenticated) {
        var header = StompHeaderAccessor.create(command);
        header.setSessionId("test-session");
        when(sessions.authorized("test-session")).thenReturn(authenticated);
        header.setDestination(destination);
        if (authenticated) { String id = UUID.randomUUID().toString(); header.setUser(() -> id); }
        header.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], header.getMessageHeaders());
    }
    @Test void rejectsOtherUsersBrokerQueues() {
        assertThatThrownBy(() -> interceptor.preSend(message(StompCommand.SUBSCRIBE, "/queue/notifications-user-other", true), null))
                .isInstanceOf(MessagingException.class);
    }
    @Test void permitsOnlyOwnUserDestination() {
        var message = message(StompCommand.SUBSCRIBE, "/user/queue/notifications", true);
        assertThat(interceptor.preSend(message, null)).isSameAs(message);
    }
    @Test void rejectsAnonymousSubscriptionsAndConnections() {
        assertThatThrownBy(() -> interceptor.preSend(message(StompCommand.SUBSCRIBE, "/user/queue/notifications", false), null)).isInstanceOf(MessagingException.class);
        assertThatThrownBy(() -> interceptor.preSend(message(StompCommand.CONNECT, null, false), null)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejectsClientSpoofingNotifications() {
        assertThatThrownBy(() -> interceptor.preSend(message(StompCommand.SEND, "/queue/notifications", true), null)).isInstanceOf(MessagingException.class);
    }
}
