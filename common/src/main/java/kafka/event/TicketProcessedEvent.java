package kafka.event;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record TicketProcessedEvent(
        UUID ticketId,
        String category,
        String priority,
        String sentiment,
        String answer,
        Instant processedAt
) {
}