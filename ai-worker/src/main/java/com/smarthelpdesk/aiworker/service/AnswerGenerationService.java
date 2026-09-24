package com.smarthelpdesk.aiworker.service;

import com.smarthelpdesk.aiworker.ai.AiClient;
import com.smarthelpdesk.aiworker.ai.PromptBuilder;
import com.smarthelpdesk.aiworker.dto.ai.AiResponse;
import com.smarthelpdesk.aiworker.dto.ai.ClassificationResult;
import com.smarthelpdesk.aiworker.dto.ai.GeneratedAnswer;
import com.smarthelpdesk.aiworker.dto.ai.PriorityResult;
import com.smarthelpdesk.aiworker.dto.ai.PromptRequest;
import com.smarthelpdesk.aiworker.dto.ai.SentimentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnswerGenerationService {

    private final PromptBuilder promptBuilder;
    private final AiClient aiClient;

    public GeneratedAnswer generate(String message, String context, ClassificationResult classificationResult, SentimentResult sentimentResult, PriorityResult priorityResult, double confidence) {

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be null or blank" );
        }

        if (classificationResult == null) {
            throw new IllegalArgumentException("classificationResult must not be null");
        }

        if (sentimentResult == null) {
            throw new IllegalArgumentException("sentimentResult must not be null");
        }

        if (priorityResult == null) {
            throw new IllegalArgumentException("priorityResult must not be null");
        }

        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be between 0.0 and 1.0");
        }

        String preparedContext =
                context == null ? "" : context;

        PromptRequest promptRequest =
                promptBuilder.buildAnswerPrompt(message, preparedContext, classificationResult, sentimentResult, priorityResult);

        AiResponse response = aiClient.chatCompletion(promptRequest);

        if (response == null) {
            throw new IllegalStateException("AI returned null response");
        }

        if (response.rawText() == null || response.rawText().isBlank()) {

            throw new IllegalStateException("AI returned empty answer");
        }

        return new GeneratedAnswer(response.rawText().trim(), confidence);
    }

    private int extractTokensUsed(AiResponse response) {

        if (response.usage() == null
                || response.usage().isEmpty()) {
            return 0;
        }

        Object totalTokens =
                response.usage().get("total_tokens");

        if (totalTokens instanceof Number number) {
            return number.intValue();
        }

        return 0;
    }
}