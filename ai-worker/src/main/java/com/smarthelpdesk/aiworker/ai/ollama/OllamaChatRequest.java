package com.smarthelpdesk.aiworker.ai.ollama;

import java.util.List;

public record OllamaChatRequest(
        String model,
        List<Message> messages,
        boolean stream,
        Options options
) {

    public record Message(
            String role,
            String content
    ) {
    }

    public record Options(
            double temperature,
            double top_p
    ) {
    }
}