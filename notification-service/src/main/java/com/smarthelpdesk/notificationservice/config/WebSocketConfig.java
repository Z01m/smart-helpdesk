package com.smarthelpdesk.notificationservice.config;
import com.smarthelpdesk.notificationservice.security.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.*;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final AccessTokenVerifier verifier;
    private final WebSocketSessionGuard sessions;
    private final String[] origins;
    public WebSocketConfig(AccessTokenVerifier verifier, WebSocketSessionGuard sessions,
            @Value("${notification.websocket.allowed-origins:http://localhost:3000}") String[] origins) {
        this.verifier = verifier; this.sessions = sessions; this.origins = origins;
        for (String origin : origins) {
            var uri = java.net.URI.create(origin);
            boolean local = "http".equals(uri.getScheme()) && java.util.Set.of("localhost", "127.0.0.1").contains(uri.getHost() == null ? "" : uri.getHost());
            if (origin.contains("*") || uri.getHost() == null || (!"https".equals(uri.getScheme()) && !local))
                throw new IllegalArgumentException("Use explicit HTTPS WebSocket origins (localhost HTTP allowed for development)");
        }
    }
    @Override public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/notifications").setAllowedOrigins(origins);
    }
    @Override public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue");
        registry.setUserDestinationPrefix("/user");
        registry.setPreservePublishOrder(true);
    }
    @Override public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new StompAuthenticationInterceptor(verifier, sessions));
        registration.taskExecutor().corePoolSize(2).maxPoolSize(8).queueCapacity(256);
    }
    @Override public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(new org.springframework.messaging.support.ChannelInterceptor() {
            @Override public org.springframework.messaging.Message<?> preSend(org.springframework.messaging.Message<?> message,
                    org.springframework.messaging.MessageChannel channel) {
                if (org.springframework.messaging.simp.SimpMessageHeaderAccessor.getMessageType(message.getHeaders()) == org.springframework.messaging.simp.SimpMessageType.MESSAGE
                        && !sessions.authorized(org.springframework.messaging.simp.SimpMessageHeaderAccessor.getSessionId(message.getHeaders()))) return null;
                return message;
            }
        });
        registration.taskExecutor().corePoolSize(2).maxPoolSize(8).queueCapacity(256);
    }
    @Override public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(16384).setSendBufferSizeLimit(65536).setSendTimeLimit(10000)
                .setTimeToFirstMessage(10000).addDecoratorFactory(handler -> new WebSocketHandlerDecorator(handler) {
                    @Override public void afterConnectionEstablished(WebSocketSession socket) throws Exception {
                        sessions.connected(socket);
                        if (socket.isOpen()) super.afterConnectionEstablished(socket);
                    }
                    @Override public void afterConnectionClosed(WebSocketSession socket, CloseStatus status) throws Exception {
                        sessions.disconnected(socket.getId());
                        super.afterConnectionClosed(socket, status);
                    }
                });
    }
}
