package kafka.event;
import java.time.Instant;
import java.util.UUID;
public record TicketStatusChangedEvent(UUID ticketId, String fromStatus, String toStatus,
        UUID changedBy, Instant changedAt) {}
