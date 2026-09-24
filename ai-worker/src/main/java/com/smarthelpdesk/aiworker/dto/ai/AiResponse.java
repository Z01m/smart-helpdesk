package com.smarthelpdesk.aiworker.dto.ai;

import java.util.Map;

public record AiResponse(
        String rawText,
        Map<String, Object> usage
) {
}