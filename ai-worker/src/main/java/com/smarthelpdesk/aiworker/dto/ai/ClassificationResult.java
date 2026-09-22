package com.smarthelpdesk.aiworker.dto.ai;

import com.smarthelpdesk.aiworker.dto.ai.enums.TicketCategory;

public record ClassificationResult(
        TicketCategory category,
        double confidence
) {
}