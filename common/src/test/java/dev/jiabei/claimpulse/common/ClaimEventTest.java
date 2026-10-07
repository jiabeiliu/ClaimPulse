package dev.jiabei.claimpulse.common;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ClaimEventTest {
    @Test void acceptsSyntheticClaimEvent() {
        ClaimEvent event = ClaimEvent.create("CL-2048", ClaimType.HEALTHCARE,
                ClaimStatus.DENIED, 300_000, Instant.parse("2026-01-15T10:00:00Z"));
        assertNotNull(event.eventId());
        assertTrue(ReviewRules.flagForReview(event.status(), event.amountCents()));
    }

    @Test void rejectsInvalidClaimIdAndAmount() {
        assertThrows(IllegalArgumentException.class, () -> ClaimEvent.create("POL-9921",
                ClaimType.AUTO, ClaimStatus.SUBMITTED, 1, Instant.now()));
        assertThrows(IllegalArgumentException.class, () -> ClaimEvent.create("CL-1234",
                ClaimType.AUTO, ClaimStatus.SUBMITTED, 0, Instant.now()));
    }

    @Test void flagsOnlyIllustrativeReviewConditions() {
        assertFalse(ReviewRules.flagForReview(ClaimStatus.APPROVED, 100_000));
        assertFalse(ReviewRules.flagForReview(ClaimStatus.DENIED, 249_999));
        assertTrue(ReviewRules.flagForReview(ClaimStatus.DENIED, 250_000));
        assertTrue(ReviewRules.flagForReview(ClaimStatus.SUBMITTED, 1_000_000));
    }
}
