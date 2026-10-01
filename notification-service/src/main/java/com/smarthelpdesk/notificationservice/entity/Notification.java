package com.smarthelpdesk.notificationservice.entity;


import com.smarthelpdesk.notificationservice.dto.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(name = "content")
    private String content;

    @Column(name = "read")
    private boolean read;


    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private Instant createdAt;

}
