package com.smarthelpdesk.notificationservice.service;
import com.smarthelpdesk.notificationservice.dto.*;
import com.smarthelpdesk.notificationservice.security.WebSocketSessionGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.*;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class NotificationDeliveryJob {
    private final JdbcTemplate jdbc;
    private final SimpMessagingTemplate messaging;
    private final SimpUserRegistry users;
    private final WebSocketSessionGuard sessions;
    private final Map<String, Long> nextSend = new ConcurrentHashMap<>();

    @Scheduled(fixedDelayString = "${notification.delivery-delay-ms:1000}")
    public void deliver() {
        Set<String> active = new HashSet<>();
        for (var user : users.getUsers()) {
            for (var session : user.getSessions()) {
                String id = session.getId(); active.add(id);
                if (!sessions.authorized(id) || session.getSubscriptions().isEmpty()
                        || nextSend.getOrDefault(id, 0L) > System.currentTimeMillis()) continue;
                try {
                    var pending = jdbc.query("""
                            SELECT * FROM notification WHERE user_id = ? AND delivered_at IS NULL AND NOT is_read
                            ORDER BY created_at, id LIMIT 100
                            """, (rs, row) -> new NotificationResponse(rs.getObject("id", UUID.class), rs.getObject("ticket_id", UUID.class),
                            NotificationType.valueOf(rs.getString("type")), rs.getString("content"), rs.getBoolean("is_read"),
                            rs.getTimestamp("created_at").toInstant()), UUID.fromString(user.getName()));
                    for (var notification : pending) {
                        if (!sessions.authorized(id)) break;
                        var headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
                        headers.setSessionId(id); headers.setLeaveMutable(true);
                        messaging.convertAndSendToUser(user.getName(), "/queue/notifications", notification, headers.getMessageHeaders());
                    }
                    nextSend.put(id, System.currentTimeMillis() + 10000);
                } catch (RuntimeException ex) { log.warn("Notification delivery failed; retry remains pending", ex); }
            }
        }
        nextSend.keySet().retainAll(active);
    }
}
