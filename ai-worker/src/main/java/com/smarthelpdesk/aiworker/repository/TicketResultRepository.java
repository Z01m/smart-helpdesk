package com.smarthelpdesk.aiworker.repository;

import com.smarthelpdesk.aiworker.entity.TicketResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TicketResultRepository
        extends JpaRepository<TicketResult, UUID> {

    Optional<TicketResult> findByTicketId(UUID ticketId);
}