package com.smarthelpdesk.apigateway.repository;

import com.smarthelpdesk.apigateway.entity.TicketStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
/**
 * Репозиторий для истории статусов тикетов.
 * Используется для сохранения и получения истории изменений статуса.
 */
public interface TicketStatusHistoryRepository extends JpaRepository<TicketStatusHistory, UUID> {
    List<TicketStatusHistory> findByTicketIdOrderByChangedAtAsc(UUID ticketId);
}