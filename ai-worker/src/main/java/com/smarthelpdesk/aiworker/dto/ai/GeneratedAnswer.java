package com.smarthelpdesk.aiworker.dto.ai;

import java.util.List;
import java.util.UUID;

public record GeneratedAnswer(
        String text,
        List<UUID> sourceArticleIds,
        int tokensUsed
) {
}