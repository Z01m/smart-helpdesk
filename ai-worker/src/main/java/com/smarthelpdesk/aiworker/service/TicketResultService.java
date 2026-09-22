package com.smarthelpdesk.aiworker.service;

import com.smarthelpdesk.aiworker.dto.ai.ClassificationResult;
import com.smarthelpdesk.aiworker.dto.ai.GeneratedAnswer;
import com.smarthelpdesk.aiworker.dto.ai.PriorityResult;
import com.smarthelpdesk.aiworker.dto.ai.SentimentResult;
import com.smarthelpdesk.aiworker.entity.AiProcessingResult;
import com.smarthelpdesk.aiworker.repository.AiProcessingResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketResultService {

    private final AiProcessingResultRepository aiProcessingResultRepository;

    public AiProcessingResult saveResult(
            UUID ticketId,
            ClassificationResult classificationResult,
            SentimentResult sentimentResult,
            PriorityResult priorityResult,
            GeneratedAnswer generatedAnswer
    ) {

        if (ticketId == null) {
            throw new IllegalArgumentException("ticketId cannot be null");
        }

        if (classificationResult == null) {
            throw new IllegalArgumentException("classificationResult cannot be null");
        }

        if (sentimentResult == null) {
            throw new IllegalArgumentException("sentimentResult cannot be null");
        }

        if (priorityResult == null) {
            throw new IllegalArgumentException("priorityResult cannot be null");
        }

        if (generatedAnswer == null) {
            throw new IllegalArgumentException("generatedAnswer cannot be null");
        }

        AiProcessingResult result = AiProcessingResult.builder()
                .ticketId(ticketId)
                .category(classificationResult.category().name())
                .sentiment(sentimentResult.sentiment().name())
                .priority(priorityResult.priority().name())
                .answer(generatedAnswer.answer())
                .processedAt(Instant.now())
                .build();

        return aiProcessingResultRepository.save(result);
    }
}