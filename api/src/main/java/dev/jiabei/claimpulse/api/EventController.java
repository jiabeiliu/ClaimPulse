package dev.jiabei.claimpulse.api;

import dev.jiabei.claimpulse.common.ClaimEvent;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventPublisher publisher;
    private final IngestGuard guard;

    public EventController(EventPublisher publisher, IngestGuard guard) {
        this.publisher = publisher;
        this.guard = guard;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> publish(@RequestHeader(value = "X-Ingest-Token", required = false) String token,
                                       @Valid @RequestBody EventInput input) {
        guard.requireWriteAccess(token);
        ClaimEvent event = publisher.publish(input);
        return Map.of("eventId", event.eventId(), "claimId", event.claimId(), "state", "accepted-by-kafka");
    }
}
