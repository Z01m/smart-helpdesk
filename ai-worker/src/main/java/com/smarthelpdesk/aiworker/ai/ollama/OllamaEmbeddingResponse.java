package com.smarthelpdesk.aiworker.ai.ollama;

import java.util.List;

public record OllamaEmbeddingResponse(
        String model,
        List<List<Double>> embeddings,
        long total_duration,
        long load_duration,
        int prompt_eval_count
) {
}