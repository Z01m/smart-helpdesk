package com.smarthelpdesk.aiworker.ai.ollama;

public record OllamaChatResponse(
        String model,
        Message message,
        boolean done,
        long prompt_eval_count,
        long eval_count
) {

    public record Message(
            String role,
            String content
    ) {
    }
}