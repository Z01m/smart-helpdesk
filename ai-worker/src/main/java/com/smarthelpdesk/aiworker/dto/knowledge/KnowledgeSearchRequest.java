package com.smarthelpdesk.aiworker.dto.knowledge;

public record KnowledgeSearchRequest(
        float[] embedding,
        int topK
) {
}
