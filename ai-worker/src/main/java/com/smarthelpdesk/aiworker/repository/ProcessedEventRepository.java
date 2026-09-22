package com.smarthelpdesk.aiworker.repository;

import com.smarthelpdesk.aiworker.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventRepository
        extends JpaRepository<ProcessedEvent, UUID> {

    boolean existsByEventId(UUID eventId);
}