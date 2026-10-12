package com.smarthelpdesk.notificationservice.security;

import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.*;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionGuard {
    private record Session(WebSocketSession socket, Instant deadline, String user) {}
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    public synchronized void connected(WebSocketSession socket) throws Exception {
        if (sessions.size() >= 1000) { socket.close(CloseStatus.SERVICE_OVERLOAD); return; }
        sessions.put(socket.getId(), new Session(socket, Instant.now().plusSeconds(10), null));
    }
    public synchronized void authenticate(String id, Authentication authentication) {
        Session current = sessions.get(id);
        if (current == null || !current.socket().isOpen() || !current.deadline().isAfter(Instant.now()))
            throw new IllegalArgumentException("WebSocket authentication window expired");
        if (current.user() != null) throw new IllegalArgumentException("WebSocket is already authenticated");
        if (!(authentication.getDetails() instanceof AccessTokenDetails details) || !details.expiresAt().isAfter(Instant.now()))
            throw new IllegalArgumentException("Expired access token");
        long userSessions = sessions.values().stream().filter(s -> authentication.getName().equals(s.user())).count();
        if (userSessions >= 5) throw new IllegalArgumentException("Too many active user connections");
        sessions.put(id, new Session(current.socket(), details.expiresAt(), authentication.getName()));
    }
    public boolean authorized(String id) {
        Session session = id == null ? null : sessions.get(id);
        return session != null && session.user() != null && session.deadline().isAfter(Instant.now()) && session.socket().isOpen();
    }
    public void disconnected(String id) { sessions.remove(id); }
    @Scheduled(fixedDelay = 1000)
    public void expire() {
        Instant now = Instant.now();
        sessions.forEach((id, session) -> {
            if (!session.deadline().isAfter(now)) {
                try { session.socket().close(CloseStatus.POLICY_VIOLATION); }
                catch (java.io.IOException ignored) { }
                finally { sessions.remove(id, session); }
            }
        });
    }
}
