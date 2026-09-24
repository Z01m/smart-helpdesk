package com.smarthelpdesk.aiworker.ai;

import com.smarthelpdesk.aiworker.ai.ollama.OllamaChatRequest;
import com.smarthelpdesk.aiworker.ai.ollama.OllamaChatResponse;
import com.smarthelpdesk.aiworker.ai.ollama.OllamaEmbeddingRequest;
import com.smarthelpdesk.aiworker.ai.ollama.OllamaEmbeddingResponse;
import com.smarthelpdesk.aiworker.config.AiClientConfig;
import com.smarthelpdesk.aiworker.dto.ai.AiResponse;
import com.smarthelpdesk.aiworker.dto.ai.PromptRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OllamaClient implements AiClient{

    private final RestClient restClient;
    private final AiClientConfig aiProperties;

    @Override
    public AiResponse chatCompletion(PromptRequest request) {
        if (request == null) {
            throw new NullPointerException("request is null");
        }

        OllamaChatRequest.Message systemMessage = new OllamaChatRequest.Message(
                "system", request.systemPrompt()
        );

        OllamaChatRequest.Message userMessage = new OllamaChatRequest.Message(
                "user", request.userPrompt()
        );

        OllamaChatRequest.Options options = new OllamaChatRequest.Options(
                request.temperature(), request.topP()
        );

        OllamaChatRequest ollamaRequest =
                new OllamaChatRequest(request.model(),
                        List.of(
                                systemMessage,
                                userMessage
                        ),
                        false,
                        options
                );

        OllamaChatResponse ollamaResponse = restClient.post()
                .uri("/api/chat")
                .body(ollamaRequest)
                .retrieve()
                .body(OllamaChatResponse.class);

        if (ollamaResponse == null) {
            throw new IllegalStateException("Ollama returned null response");
        }

        if (ollamaResponse.message() == null) {
            throw new IllegalStateException("Ollama response does not contain message");
        }

        String content = ollamaResponse.message().content();

        if (content == null || content.isBlank()) {
            throw new IllegalStateException("Ollama returned empty response");
        }

        Map<String, Object> usage = Map.of(
                "promptTokens",
                ollamaResponse.prompt_eval_count(),
                "completionTokens",
                ollamaResponse.eval_count()
        );

        return new AiResponse(content, usage);
    }

    @Override
    public float[] embed(String text) {

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "text cannot be null or blank"
            );
        }

        OllamaEmbeddingRequest request =
                new OllamaEmbeddingRequest(
                        aiProperties.getEmbeddingModel(),
                        text
                );

        OllamaEmbeddingResponse response =
                restClient
                        .post()
                        .uri("/api/embed")
                        .body(request)
                        .retrieve()
                        .body(OllamaEmbeddingResponse.class);

        if (response == null) {
            throw new IllegalStateException(
                    "Ollama returned null embedding response"
            );
        }

        if (response.embeddings() == null
                || response.embeddings().isEmpty()) {

            throw new IllegalStateException(
                    "Ollama response does not contain embeddings"
            );
        }

        List<Double> embedding =
                response.embeddings().get(0);

        if (embedding == null || embedding.isEmpty()) {
            throw new IllegalStateException(
                    "Ollama returned empty embedding"
            );
        }

        float[] result = new float[embedding.size()];

        for (int i = 0; i < embedding.size(); i++) {
            result[i] = embedding.get(i).floatValue();
        }

        return result;
    }
}
