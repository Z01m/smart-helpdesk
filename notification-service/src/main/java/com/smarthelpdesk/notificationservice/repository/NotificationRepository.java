package com.smarthelpdesk.notificationservice.repository;

import com.smarthelpdesk.notificationservice.entity.Notification;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    Page<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "UPDATE notification SET delivered_at = COALESCE(delivered_at, now()), ws_published = TRUE WHERE id = :id AND user_id = :user", nativeQuery = true)
    int acknowledge(@org.springframework.data.repository.query.Param("id") UUID id,
                    @org.springframework.data.repository.query.Param("user") UUID user);

}
