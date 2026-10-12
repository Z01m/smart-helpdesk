package com.smarthelpdesk.notificationservice.repository;

import com.smarthelpdesk.notificationservice.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
    boolean existsByEventId(UUID eventId);

    // Unique primary key arbitrates concurrent deliveries; the claim rolls back with the notification.
    @Modifying
    @Query(value = "INSERT INTO processed_event(event_id) VALUES (:id) ON CONFLICT DO NOTHING", nativeQuery = true)
    int claim(@Param("id") UUID eventId);
}
