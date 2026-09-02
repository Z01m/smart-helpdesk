package kafka;

public enum KafkaTopics {

    TICKET_CREATED("ticket.created"),
    TICKET_PROCESSED("ticket.processed"),
    TICKET_STATUS_CHANGED("ticket.status.changed"),
    TICKET_ASSIGNED("ticket.assigned"),
    TICKET_DLQ("ticket.dlq");

    private final String topicName;

    KafkaTopics(String topicName) {
        this.topicName = topicName;
    }

    public String getTopicName() {
        return topicName;
    }
}