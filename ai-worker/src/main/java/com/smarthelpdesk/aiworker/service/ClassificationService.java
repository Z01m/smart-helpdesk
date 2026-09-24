package com.smarthelpdesk.aiworker.service;

import com.smarthelpdesk.aiworker.ai.AiClient;
import com.smarthelpdesk.aiworker.ai.AiResponseParser;
import com.smarthelpdesk.aiworker.ai.PromptBuilder;
import com.smarthelpdesk.aiworker.dto.ai.AiResponse;
import com.smarthelpdesk.aiworker.dto.ai.ClassificationResult;
import com.smarthelpdesk.aiworker.dto.ai.PromptRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClassificationService {

    private final PromptBuilder promptBuilder;
    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;

    public ClassificationResult classify(String message) {

        PromptRequest request =
                promptBuilder.buildClassificationPrompt(message);

        AiResponse response =
                aiClient.chatCompletion(request);

        return aiResponseParser.parseClassification(
                response.rawText()
        );
    }
}