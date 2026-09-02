package com.smarthelpdesk.apigateway.repository;

import com.smarthelpdesk.apigateway.entity.OutboxEvent;
import jdk.jfr.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    public Optional<OutboxEvent> findById(UUID id);
    public List<OutboxEvent> findByPublishedFalse();

}
