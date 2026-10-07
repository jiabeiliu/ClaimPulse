package dev.jiabei.claimpulse.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jiabei.claimpulse.common.ClaimStatus;
import dev.jiabei.claimpulse.common.ClaimType;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.server.ResponseStatusException;

class EventPublisherTest {
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    private final EventPublisher publisher = new EventPublisher(kafka,
            new ObjectMapper().findAndRegisterModules(), "claim-events");

    @Test void sendsValidatedEventWithClaimIdAsPartitionKey() {
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
        var event = publisher.publish(new EventInput("CL-2048", ClaimType.HEALTHCARE,
                ClaimStatus.DENIED, 300_000L, Instant.parse("2026-01-15T10:00:00Z")));
        assertEquals("CL-2048", event.claimId());
        verify(kafka).send(eq("claim-events"), eq("CL-2048"), contains("\"status\":\"DENIED\""));
    }

    @Test void failedKafkaAcknowledgementDoesNotClaimAcceptance() {
        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("broker down"));
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(failed);
        assertThrows(ResponseStatusException.class, () -> publisher.publish(new EventInput("CL-2048",
                ClaimType.HEALTHCARE, ClaimStatus.DENIED, 300_000L, Instant.now())));
    }
}
