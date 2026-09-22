package com.smarthelpdesk.aiworker.dto.ai;

import com.smarthelpdesk.aiworker.dto.ai.enums.SentimentType;

public record SentimentResult(
        SentimentType sentiment,
        double confidence
) {
}