package dev.jiabei.claimpulse.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class IngestGuard {
    private final String token;
    private final boolean demoEnabled;

    public IngestGuard(@Value("${claimpulse.ingest-token:}") String token,
                       @Value("${claimpulse.demo-enabled:false}") boolean demoEnabled) {
        this.token = token;
        this.demoEnabled = demoEnabled;
    }

    public void requireWriteAccess(String supplied) {
        if (token.isBlank()) {
            if (demoEnabled) return; // Local-only demo mode; never enable on a public deployment.
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Ingest token is not configured");
        }
        if (supplied == null || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid ingest token");
        }
    }
}
