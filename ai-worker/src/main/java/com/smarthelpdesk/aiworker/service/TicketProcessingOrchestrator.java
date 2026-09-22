package com.smarthelpdesk.aiworker.service;

import com.smarthelpdesk.aiworker.dto.ai.ClassificationResult;
import com.smarthelpdesk.aiworker.dto.ai.GeneratedAnswer;
import com.smarthelpdesk.aiworker.dto.ai.PriorityResult;
import com.smarthelpdesk.aiworker.dto.ai.SentimentResult;
import com.smarthelpdesk.aiworker.dto.ai.enums.CustomerTier;
import com.smarthelpdesk.aiworker.entity.AiProcessingResult;
import com.smarthelpdesk.aiworker.repository.AiProcessingResultRepository;
import kafka.event.TicketCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TicketProcessingOrchestrator {

    private final ClassificationService classificationService;
    private final SentimentAnalysisService sentimentAnalysisService;
    private final PriorityCalculationService  priorityCalculationService;
    private final AnswerGenerationService answerGenerationService;
    private final TicketResultService ticketResultService;

    public AiProcessingResult process(TicketCreatedEvent event) {

        if(event == null) throw new NullPointerException("event is null");
        ClassificationResult classificationResult = classificationService.classify(event.message());
        SentimentResult sentimentResult = sentimentAnalysisService.analyze(event.message());
        PriorityResult priorityResult = priorityCalculationService.calculate(CustomerTier.valueOf(event.customerTier()), classificationResult, sentimentResult);
        GeneratedAnswer generatedAnswer = answerGenerationService.generate(event.message(),  classificationResult, sentimentResult, priorityResult);

        return ticketResultService.saveResult(
                event.ticketId(),
                classificationResult,
                sentimentResult,
                priorityResult,
                generatedAnswer
        );
    }
}
