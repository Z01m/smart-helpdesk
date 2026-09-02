package com.smarthelpdesk.apigateway.repository;

import com.smarthelpdesk.apigateway.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;
/**
 * Репозиторий для работы с сущностью Ticket.
 * Предоставляет сохранение и поиск тикетов через Spring Data JPA.
 */
public interface TicketRepository extends JpaRepository<Ticket, UUID>,
        JpaSpecificationExecutor<Ticket> {

    Page<Ticket> findByUserId(UUID userId, Pageable pageable);
}