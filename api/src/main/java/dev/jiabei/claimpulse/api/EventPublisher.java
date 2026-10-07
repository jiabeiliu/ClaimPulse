package dev.jiabei.claimpulse.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jiabei.claimpulse.common.ClaimEvent;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EventPublisher {
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;
    private final String topic;

    public EventPublisher(KafkaTemplate<String, String> kafka, ObjectMapper json,
                          @Value("${claimpulse.topic}") String topic) {
        this.kafka = kafka;
        this.json = json;
        this.topic = topic;
    }

    public ClaimEvent publish(EventInput input) {
        ClaimEvent event = ClaimEvent.create(input.claimId(), input.claimType(), input.status(),
                input.amountCents(), input.occurredAt() == null ? Instant.now() : input.occurredAt());
        try {
            kafka.send(topic, event.claimId(), json.writeValueAsString(event)).get(5, TimeUnit.SECONDS);
            return event;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Event publish interrupted", exception);
        } catch (TimeoutException | java.util.concurrent.ExecutionException | JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Kafka publish failed", exception);
        }
    }
}
