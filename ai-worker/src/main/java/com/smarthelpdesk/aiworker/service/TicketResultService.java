package com.smarthelpdesk.aiworker.service;

import com.smarthelpdesk.aiworker.entity.TicketResult;
import com.smarthelpdesk.aiworker.repository.TicketResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketResultService {

    private final TicketResultRepository ticketResultRepository;

    public TicketResult save(TicketResult ticketResult) {

        if (ticketResult == null) {
            throw new IllegalArgumentException(
                    "ticketResult cannot be null"
            );
        }

        return ticketResultRepository.save(ticketResult);
    }

    public Optional<TicketResult> findByTicketId(UUID ticketId) {

        if (ticketId == null) {
            throw new IllegalArgumentException(
                    "ticketId cannot be null"
            );
        }

        return ticketResultRepository.findByTicketId(ticketId);
    }
}