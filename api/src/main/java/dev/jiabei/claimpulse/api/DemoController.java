package dev.jiabei.claimpulse.api;

import dev.jiabei.claimpulse.common.ClaimStatus;
import dev.jiabei.claimpulse.common.ClaimType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** Only registered when local demo mode is explicitly enabled. */
@RestController
@RequestMapping("/api/demo")
@ConditionalOnProperty(name = "claimpulse.demo-enabled", havingValue = "true")
public class DemoController {
    private final EventPublisher publisher;

    public DemoController(EventPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping("/generate")
    public Map<String, Object> generate(@RequestParam(defaultValue = "20") int count) {
        if (count < 1 || count > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "count must be between 1 and 100");
        }
        List<String> ids = new ArrayList<>();
        ClaimStatus[] statuses = ClaimStatus.values();
        ClaimType[] types = ClaimType.values();
        for (int i = 0; i < count; i++) {
            String claimId = "CL-" + (2000 + i % 20);
            EventInput input = new EventInput(claimId, types[i % types.length],
                    statuses[i % statuses.length], 50_000L + (i * 75_000L),
                    Instant.now().minusSeconds(count - i));
            ids.add(publisher.publish(input).eventId());
        }
        return Map.of("produced", ids.size(), "eventIds", ids);
    }
}
