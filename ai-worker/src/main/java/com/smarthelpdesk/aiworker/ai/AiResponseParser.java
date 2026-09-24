package com.smarthelpdesk.aiworker.ai;

import com.smarthelpdesk.aiworker.dto.ai.ClassificationResult;
import com.smarthelpdesk.aiworker.dto.ai.SentimentResult;
import com.smarthelpdesk.aiworker.dto.ai.enums.SentimentType;
import com.smarthelpdesk.aiworker.dto.ai.enums.TicketCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class AiResponseParser {

    private final ObjectMapper objectMapper;

    public ClassificationResult parseClassification(String rawResponse) {

        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalArgumentException(
                    "AI classification response cannot be null or blank"
            );
        }

        try {

            ClassificationPayload payload =
                    objectMapper.readValue(
                            rawResponse,
                            ClassificationPayload.class
                    );

            if (payload.category() == null
                    || payload.category().isBlank()) {

                throw new IllegalArgumentException(
                        "AI response does not contain category"
                );
            }

            if (payload.confidence() < 0.0
                    || payload.confidence() > 1.0) {

                throw new IllegalArgumentException(
                        "classification confidence must be between 0.0 and 1.0"
                );
            }

            TicketCategory category =
                    TicketCategory.valueOf(
                            payload.category()
                                    .trim()
                                    .toUpperCase(Locale.ROOT)
                    );

            return new ClassificationResult(
                    category,
                    payload.confidence()
            );

        } catch (Exception exception) {

            throw new IllegalArgumentException(
                    "Cannot parse AI classification response: "
                            + rawResponse,
                    exception
            );
        }
    }

    public SentimentResult parseSentiment(String rawResponse) {

        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalArgumentException(
                    "AI sentiment response cannot be null or blank"
            );
        }

        try {

            SentimentPayload payload =
                    objectMapper.readValue(
                            rawResponse,
                            SentimentPayload.class
                    );

            if (payload.sentiment() == null
                    || payload.sentiment().isBlank()) {

                throw new IllegalArgumentException(
                        "AI response does not contain sentiment"
                );
            }

            if (payload.score() < -1.0
                    || payload.score() > 1.0) {

                throw new IllegalArgumentException(
                        "sentiment score must be between -1.0 and 1.0"
                );
            }

            SentimentType sentiment =
                    SentimentType.valueOf(
                            payload.sentiment()
                                    .trim()
                                    .toUpperCase(Locale.ROOT)
                    );

            return new SentimentResult(
                    sentiment,
                    payload.score()
            );

        } catch (Exception exception) {

            throw new IllegalArgumentException(
                    "Cannot parse AI sentiment response: "
                            + rawResponse,
                    exception
            );
        }
    }

    public String parseAnswer(String rawResponse) {

        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalArgumentException(
                    "AI answer cannot be null or blank"
            );
        }

        return rawResponse.trim();
    }

    private record ClassificationPayload(
            String category,
            double confidence
    ) {
    }

    private record SentimentPayload(
            String sentiment,
            double score
    ) {
    }
}