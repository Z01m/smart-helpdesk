package com.smarthelpdesk.apigateway.repository;

import com.smarthelpdesk.apigateway.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "INSERT INTO processed_event(event_id, processed_at) VALUES (:eventId, now()) ON CONFLICT(event_id) DO NOTHING", nativeQuery = true)
    int claim(@org.springframework.data.repository.query.Param("eventId") UUID eventId);
    boolean existsByEventId(UUID eventId);
}
