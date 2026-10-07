package dev.jiabei.claimpulse.api;

import dev.jiabei.claimpulse.common.ClaimStatus;
import dev.jiabei.claimpulse.common.ClaimType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

public record EventInput(
        @NotBlank @Pattern(regexp = "CL-[0-9]{4,8}") String claimId,
        @NotNull ClaimType claimType,
        @NotNull ClaimStatus status,
        @NotNull @Positive Long amountCents,
        Instant occurredAt) {}
