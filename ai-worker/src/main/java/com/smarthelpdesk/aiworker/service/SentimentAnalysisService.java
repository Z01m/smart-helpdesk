package com.smarthelpdesk.aiworker.service;

import com.smarthelpdesk.aiworker.ai.AiClient;
import com.smarthelpdesk.aiworker.ai.AiResponseParser;
import com.smarthelpdesk.aiworker.ai.PromptBuilder;
import com.smarthelpdesk.aiworker.dto.ai.AiResponse;
import com.smarthelpdesk.aiworker.dto.ai.PromptRequest;
import com.smarthelpdesk.aiworker.dto.ai.SentimentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SentimentAnalysisService {

    private final PromptBuilder promptBuilder;
    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;

    public SentimentResult analyze(String message) {

        PromptRequest request =
                promptBuilder.buildSentimentPrompt(message);

        AiResponse response =
                aiClient.chatCompletion(request);

        return aiResponseParser.parseSentiment(
                response.rawText()
        );
    }
}