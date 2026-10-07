package dev.jiabei.claimpulse.common;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** No customer identifiers or medical details belong in this event contract. */
public record ClaimEvent(
        String eventId,
        String claimId,
        ClaimType claimType,
        ClaimStatus status,
        long amountCents,
        Instant occurredAt) {

    public ClaimEvent {
        UUID.fromString(Objects.requireNonNull(eventId, "eventId"));
        if (claimId == null || !claimId.matches("CL-[0-9]{4,8}")) {
            throw new IllegalArgumentException("claimId must look like CL-1234");
        }
        Objects.requireNonNull(claimType, "claimType");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(occurredAt, "occurredAt");
        if (amountCents <= 0 || amountCents > 100_000_000_00L) {
            throw new IllegalArgumentException("amountCents is out of range");
        }
    }

    public static ClaimEvent create(String claimId, ClaimType claimType, ClaimStatus status,
                                    long amountCents, Instant occurredAt) {
        return new ClaimEvent(UUID.randomUUID().toString(), claimId, claimType, status,
                amountCents, occurredAt);
    }
}
