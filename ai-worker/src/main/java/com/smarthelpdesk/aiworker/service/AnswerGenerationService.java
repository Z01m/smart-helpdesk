package com.smarthelpdesk.aiworker.service;

import com.smarthelpdesk.aiworker.ai.AiClient;
import com.smarthelpdesk.aiworker.ai.AiResponseParser;
import com.smarthelpdesk.aiworker.ai.PromptBuilder;
import com.smarthelpdesk.aiworker.dto.ai.AiResponse;
import com.smarthelpdesk.aiworker.dto.ai.ClassificationResult;
import com.smarthelpdesk.aiworker.dto.ai.GeneratedAnswer;
import com.smarthelpdesk.aiworker.dto.ai.PriorityResult;
import com.smarthelpdesk.aiworker.dto.ai.PromptRequest;
import com.smarthelpdesk.aiworker.dto.ai.SentimentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnswerGenerationService {

    private final PromptBuilder promptBuilder;
    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;

    public GeneratedAnswer generate(
            String message,
            ClassificationResult classificationResult,
            SentimentResult sentimentResult,
            PriorityResult priorityResult
    ) {

        PromptRequest request =
                promptBuilder.buildAnswerPrompt(
                        message,
                        classificationResult,
                        sentimentResult,
                        priorityResult
                );

        AiResponse response =
                aiClient.chatCompletion(request);

        String answer =
                aiResponseParser.parseAnswer(
                        response.rawText()
                );

        return new GeneratedAnswer(
                answer,
                1.0
        );
    }
}