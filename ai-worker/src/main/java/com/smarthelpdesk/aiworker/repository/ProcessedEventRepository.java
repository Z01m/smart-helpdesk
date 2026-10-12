package com.smarthelpdesk.aiworker.repository;

import com.smarthelpdesk.aiworker.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventRepository
        extends JpaRepository<ProcessedEvent, UUID> {

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value="INSERT INTO processed_event(event_id,processed_at) VALUES (:id,now()) ON CONFLICT(event_id) DO NOTHING",nativeQuery=true)
    int claim(@org.springframework.data.repository.query.Param("id") UUID id);
    boolean existsByEventId(UUID eventId);
}