package com.smarthelpdesk.aiworker.service;

import com.smarthelpdesk.aiworker.entity.AiProcessingResult;
import com.smarthelpdesk.aiworker.repository.AiProcessingResultRepository;
import kafka.event.TicketCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AiProcessingService {

    private final AiProcessingResultRepository aiProcessingResultRepository;

    public AiProcessingResult process(TicketCreatedEvent event) {

        String category = "GENERAL";
        String priority = "MEDIUM";
        String sentiment = "NEUTRAL";
        String answer = "Test AI response";

        AiProcessingResult result = AiProcessingResult.builder()
                .ticketId(event.ticketId())
                .category(category)
                .priority(priority)
                .sentiment(sentiment)
                .answer(answer)
                .processedAt(Instant.now())
                .build();

        return aiProcessingResultRepository.save(result);
    }
}