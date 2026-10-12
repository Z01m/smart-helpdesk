package com.smarthelpdesk.notificationservice.config;
import com.smarthelpdesk.notificationservice.security.*;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;

public class StompAuthenticationInterceptor implements ChannelInterceptor {
    private final AccessTokenVerifier verifier;
    private final WebSocketSessionGuard sessions;
    public StompAuthenticationInterceptor(AccessTokenVerifier verifier, WebSocketSessionGuard sessions) {
        this.verifier = verifier; this.sessions = sessions;
    }
    @Override public Message<?> preSend(Message<?> message, MessageChannel channel) {
        var header = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (header == null) throw new MessagingException("Missing STOMP headers");
        var command = header.getCommand();
        if (command == StompCommand.CONNECT || command == StompCommand.STOMP) {
            var authentication = verifier.authenticate(header.getFirstNativeHeader("Authorization"));
            sessions.authenticate(header.getSessionId(), authentication);
            header.setUser(authentication);
        } else if (command == StompCommand.DISCONNECT) {
            return message;
        } else {
            if (!sessions.authorized(header.getSessionId())) throw new MessagingException("Authentication expired or missing");
            if (command == StompCommand.SUBSCRIBE) {
                if (header.getUser() == null || !"/user/queue/notifications".equals(header.getDestination()))
                    throw new MessagingException("Only your notification queue is allowed");
            } else if (command != null && command != StompCommand.UNSUBSCRIBE) {
                throw new MessagingException("Unsupported client command");
            }
        }
        return message;
    }
}
