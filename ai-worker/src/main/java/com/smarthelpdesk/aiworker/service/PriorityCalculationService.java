package com.smarthelpdesk.aiworker.service;

import com.smarthelpdesk.aiworker.dto.ai.ClassificationResult;
import com.smarthelpdesk.aiworker.dto.ai.PriorityResult;
import com.smarthelpdesk.aiworker.dto.ai.SentimentResult;
import com.smarthelpdesk.aiworker.dto.ai.enums.CustomerTier;
import com.smarthelpdesk.aiworker.dto.ai.enums.PriorityLevel;
import com.smarthelpdesk.aiworker.dto.ai.enums.SentimentType;
import com.smarthelpdesk.aiworker.dto.ai.enums.TicketCategory;
import org.springframework.stereotype.Service;

@Service
public class PriorityCalculationService {

    public PriorityResult calculate(
            CustomerTier customerTier,
            ClassificationResult classificationResult,
            SentimentResult sentimentResult
    ) {

        if (customerTier == null) {
            throw new IllegalArgumentException("customerTier cannot be null");
        }

        if (classificationResult == null) {
            throw new IllegalArgumentException("classificationResult cannot be null");
        }

        if (sentimentResult == null) {
            throw new IllegalArgumentException("sentimentResult cannot be null");
        }

        TicketCategory category = classificationResult.category();
        SentimentType sentiment = sentimentResult.sentiment();

        if (category == TicketCategory.SECURITY) {
            return new PriorityResult(PriorityLevel.CRITICAL);
        }

        if (customerTier == CustomerTier.VIP) {
            return new PriorityResult(PriorityLevel.HIGH);
        }

        PriorityLevel priority = switch (sentiment) {
            case ANGRY, NEGATIVE -> PriorityLevel.HIGH;
            case POSITIVE, NEUTRAL -> PriorityLevel.NORMAL;
        };

        return new PriorityResult(priority);
    }
}