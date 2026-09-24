package com.smarthelpdesk.aiworker.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthelpdesk.aiworker.dto.ai.ClassificationResult;
import com.smarthelpdesk.aiworker.dto.ai.GeneratedAnswer;
import com.smarthelpdesk.aiworker.dto.ai.PriorityResult;
import com.smarthelpdesk.aiworker.dto.ai.SentimentResult;
import com.smarthelpdesk.aiworker.dto.ai.enums.CustomerTier;
import com.smarthelpdesk.aiworker.dto.knowledge.ScoredChunk;
import com.smarthelpdesk.aiworker.entity.TicketResult;
import com.smarthelpdesk.aiworker.rag.RagPipeline;
import kafka.event.TicketCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketProcessingOrchestrator {

    private static final int MAX_CONTEXT_TOKENS = 1500;

    private static final String FALLBACK_ANSWER =
            "Ваш запрос принят. Для точного ответа требуется помощь оператора.";

    private final ClassificationService classificationService;
    private final SentimentAnalysisService sentimentAnalysisService;
    private final PriorityCalculationService priorityCalculationService;
    private final AnswerGenerationService answerGenerationService;
    private final TicketResultService ticketResultService;
    private final RagPipeline ragPipeline;
    private final ObjectMapper objectMapper;

    public TicketResult process(TicketCreatedEvent event) {

        if (event == null) {
            throw new IllegalArgumentException("event cannot be null");
        }

        ClassificationResult classificationResult =
                classificationService.classify(event.message());

        SentimentResult sentimentResult =
                sentimentAnalysisService.analyze(event.message());

        PriorityResult priorityResult =
                priorityCalculationService.calculate(
                        CustomerTier.valueOf(event.customerTier()),
                        classificationResult,
                        sentimentResult
                );

        List<ScoredChunk> scoredChunks =
                ragPipeline.retrieveContext(event.message());

        double confidence =
                ragPipeline.getMaxScore(scoredChunks);

        GeneratedAnswer generatedAnswer;
        List<ScoredChunk> usedChunks;

        if (ragPipeline.hasRelevantContext(scoredChunks)) {

            String context =
                    ragPipeline.buildContext(
                            scoredChunks,
                            MAX_CONTEXT_TOKENS
                    );

            List<UUID> sourceArticleIds =
                    extractSourceArticleIds(scoredChunks);

            GeneratedAnswer aiAnswer =
                    answerGenerationService.generate(
                            event.message(),
                            context,
                            classificationResult,
                            sentimentResult,
                            priorityResult,
                            confidence
                    );

            generatedAnswer = new GeneratedAnswer(
                    aiAnswer.text(),
                    sourceArticleIds,
                    aiAnswer.tokensUsed()
            );

            usedChunks = scoredChunks;

        } else {

            generatedAnswer = new GeneratedAnswer(
                    FALLBACK_ANSWER,
                    List.of(),
                    0
            );

            usedChunks = List.of();
        }

        TicketResult ticketResult = TicketResult.builder()
                .ticketId(event.ticketId())
                .category(classificationResult.category().name())
                .sentiment(String.valueOf(sentimentResult.sentiment()))
                .priority(String.valueOf(priorityResult.priority()))
                .generatedAnswer(generatedAnswer.text())
                .confidence(BigDecimal.valueOf(confidence))
                .contextSources(serializeContextSources(usedChunks))
                .build();

        return ticketResultService.save(ticketResult);
    }

    private List<UUID> extractSourceArticleIds(
            List<ScoredChunk> chunks
    ) {
        return chunks.stream()
                .filter(Objects::nonNull)
                .map(ScoredChunk::articleId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private String serializeContextSources(
            List<ScoredChunk> chunks
    ) {

        List<ContextSource> sources = chunks.stream()
                .filter(Objects::nonNull)
                .map(chunk -> new ContextSource(
                        chunk.articleId(),
                        chunk.title(),
                        chunk.score()
                ))
                .toList();

        try {
            return objectMapper.writeValueAsString(sources);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize RAG context sources",
                    exception
            );
        }
    }

    private record ContextSource(
            UUID articleId,
            String title,
            double score
    ) {
    }
}