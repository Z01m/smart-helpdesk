package kafka.event;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record TicketProcessedEvent(
        UUID ticketId,
        String category,
        String priority,
        String sentiment,
        BigDecimal confidence,
        String generatedAnswer,
        List<ContextSource> contextSources,
        Instant processedAt
) {

    public record ContextSource(
            UUID articleId,
            String title,
            double score
    ) {
    }
}