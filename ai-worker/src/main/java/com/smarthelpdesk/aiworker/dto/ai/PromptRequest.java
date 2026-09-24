package com.smarthelpdesk.aiworker.dto.ai;

public record PromptRequest(
        String systemPrompt,
        String userPrompt,
        double temperature,
        double topP,
        String model
) {
}