package dev.jiabei.claimpulse.common;

/** Illustrative triage rules, not an insurance decision or trained ML model. */
public final class ReviewRules {
    public static final long HIGH_AMOUNT_CENTS = 1_000_000;
    public static final long DENIED_AMOUNT_CENTS = 250_000;

    private ReviewRules() {}

    public static boolean flagForReview(ClaimStatus status, long amountCents) {
        return amountCents >= HIGH_AMOUNT_CENTS
                || (status == ClaimStatus.DENIED && amountCents >= DENIED_AMOUNT_CENTS);
    }
}
