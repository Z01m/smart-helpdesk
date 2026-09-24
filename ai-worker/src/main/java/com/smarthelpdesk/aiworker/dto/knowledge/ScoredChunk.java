package com.smarthelpdesk.aiworker.dto.knowledge;

import java.util.UUID;

public record ScoredChunk(
        UUID articleId,
        String title,
        String text,
        double score
) {
}
