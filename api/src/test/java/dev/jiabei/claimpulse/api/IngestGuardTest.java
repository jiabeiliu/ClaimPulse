package dev.jiabei.claimpulse.api;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class IngestGuardTest {
    @Test void cloudModeRequiresConfiguredToken() {
        assertThrows(ResponseStatusException.class, () -> new IngestGuard("", false).requireWriteAccess(null));
        assertThrows(ResponseStatusException.class, () -> new IngestGuard("secret", false).requireWriteAccess("wrong"));
        assertDoesNotThrow(() -> new IngestGuard("secret", false).requireWriteAccess("secret"));
    }

    @Test void onlyExplicitLocalDemoAllowsTokenlessWrites() {
        assertDoesNotThrow(() -> new IngestGuard("", true).requireWriteAccess(null));
    }
}
