package com.smarthelpdesk.aiworker.dto.processing;

import com.smarthelpdesk.aiworker.entity.TicketResult;

public record TicketProcessingOutcome(
        TicketResult ticketResult,
        boolean requiresHumanReview
) {
}