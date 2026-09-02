package kafka.event;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;
@Builder
public record TicketCreatedEvent(
        UUID eventId,
        UUID ticketId,
        UUID userId,
        String message,
        String customerTier,
        Instant createdAt
) {
}
