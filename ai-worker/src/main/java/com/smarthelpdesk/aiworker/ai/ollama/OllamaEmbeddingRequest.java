package com.smarthelpdesk.aiworker.ai.ollama;

public record OllamaEmbeddingRequest(
        String model,
        String input
) {
}