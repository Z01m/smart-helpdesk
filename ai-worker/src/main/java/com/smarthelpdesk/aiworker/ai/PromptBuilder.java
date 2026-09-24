package com.smarthelpdesk.aiworker.ai;

import com.smarthelpdesk.aiworker.config.AiClientConfig;
import com.smarthelpdesk.aiworker.dto.ai.ClassificationResult;
import com.smarthelpdesk.aiworker.dto.ai.PriorityResult;
import com.smarthelpdesk.aiworker.dto.ai.PromptRequest;
import com.smarthelpdesk.aiworker.dto.ai.SentimentResult;
import com.smarthelpdesk.aiworker.dto.ai.enums.SentimentType;
import com.smarthelpdesk.aiworker.dto.ai.enums.TicketCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class PromptBuilder {

    private final AiClientConfig aiClientConfig;

    public PromptRequest buildClassificationPrompt(String message) {

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "message cannot be null or blank"
            );
        }

        String categories = String.join(
                ", ",
                Arrays.stream(TicketCategory.values())
                        .map(Enum::name)
                        .toList()
        );

        String userPrompt = """
                Available categories:
                %s

                User message:
                %s
                """
                .formatted(
                        categories,
                        message
                );

        return new PromptRequest(
                SystemPromptTemplates.CLASSIFICATION_SYSTEM_PROMPT,
                userPrompt,
                aiClientConfig.getClassificationTemperature(),
                aiClientConfig.getClassificationTopP(),
                aiClientConfig.getModel()
        );
    }

    public PromptRequest buildSentimentPrompt(String message) {

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "message cannot be null or blank"
            );
        }

        String sentiments = String.join(
                ", ",
                Arrays.stream(SentimentType.values())
                        .map(Enum::name)
                        .toList()
        );

        String userPrompt = """
                Available sentiments:
                %s

                User message:
                %s
                """
                .formatted(
                        sentiments,
                        message
                );

        return new PromptRequest(
                SystemPromptTemplates.SENTIMENT_SYSTEM_PROMPT,
                userPrompt,
                aiClientConfig.getClassificationTemperature(),
                aiClientConfig.getClassificationTopP(),
                aiClientConfig.getModel()
        );
    }

    public PromptRequest buildAnswerPrompt(
            String message,
            ClassificationResult classificationResult,
            SentimentResult sentimentResult,
            PriorityResult priorityResult
    ) {

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "message cannot be null or blank"
            );
        }

        if (classificationResult == null) {
            throw new IllegalArgumentException(
                    "classificationResult cannot be null"
            );
        }

        if (sentimentResult == null) {
            throw new IllegalArgumentException(
                    "sentimentResult cannot be null"
            );
        }

        if (priorityResult == null) {
            throw new IllegalArgumentException(
                    "priorityResult cannot be null"
            );
        }

        String userPrompt = """
                User message:
                %s

                Ticket analysis:
                Category: %s
                Sentiment: %s
                Priority: %s
                """
                .formatted(
                        message,
                        classificationResult.category().name(),
                        sentimentResult.sentiment().name(),
                        priorityResult.priority().name()
                );

        return new PromptRequest(
                SystemPromptTemplates.ANSWER_SYSTEM_PROMPT,
                userPrompt,
                aiClientConfig.getAnswerTemperature(),
                aiClientConfig.getAnswerTopP(),
                aiClientConfig.getModel()
        );
    }
}