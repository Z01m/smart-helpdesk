package com.smarthelpdesk.aiworker.dto.knowledge;

import java.util.List;

public record KnowledgeSearchResponse(
        List<ScoredChunk> chunks
) {
}
