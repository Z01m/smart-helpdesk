package com.smarthelpdesk.aiworker.producer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;
import org.apache.kafka.clients.producer.ProducerRecord;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ResultOutboxPublisher {
    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String,String> kafka;
    private record Pending(UUID id,UUID ticket,String payload,String correlation) {}
    @Scheduled(fixedDelayString="${ai.outbox.poll-ms:1000}")
    public void publish() {
        var batch=jdbc.query("SELECT * FROM result_outbox WHERE published_at IS NULL ORDER BY created_at,id LIMIT 20",
            (rs,n)->new Pending(rs.getObject("id",UUID.class),rs.getObject("ticket_id",UUID.class),rs.getString("payload"),rs.getString("correlation_id")));
        for(var item:batch) {
            try {
                var record=new ProducerRecord<String,String>("ticket.processed",item.ticket().toString(),item.payload());
                record.headers().add("correlationId",item.correlation().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                kafka.send(record).get(10,TimeUnit.SECONDS);
                jdbc.update("UPDATE result_outbox SET published_at=now() WHERE id=? AND published_at IS NULL",item.id());
            } catch(InterruptedException ex) { Thread.currentThread().interrupt();return; }
              catch(Exception ex) { log.warn("AI result publication failed; saved event {} will be retried",item.id());return; }
        }
    }
}
