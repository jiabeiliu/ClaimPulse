import test from "node:test";
import assert from "node:assert/strict";
import {
  SAMPLE_EVENTS, acceptEvent, createEvent, dollarsToCents, emptyState,
  eventsForClaim, flagForReview, rejectRaw, summarize,
} from "../docs/simulator.js";

const id = (number) => `00000000-0000-4000-8000-${String(number).padStart(12, "0")}`;

test("dollar parser uses exact cents and rejects unsupported amounts", () => {
  assert.equal(dollarsToCents("3200.50"), 320050);
  assert.equal(dollarsToCents("0.01"), 1);
  assert.throws(() => dollarsToCents("10.999"));
  assert.throws(() => dollarsToCents("0"));
  assert.throws(() => dollarsToCents("100000000.01"));
});

test("review rules match the Java thresholds", () => {
  assert.equal(flagForReview("DENIED", 249999), false);
  assert.equal(flagForReview("DENIED", 250000), true);
  assert.equal(flagForReview("APPROVED", 999999), false);
  assert.equal(flagForReview("APPROVED", 1000000), true);
});

test("sample scenario updates metrics and claim history", () => {
  let state = emptyState();
  SAMPLE_EVENTS.forEach((input, index) => {
    state = acceptEvent(state, createEvent(input, id(index + 1), "2026-01-15T12:00:00Z"));
  });
  assert.deepEqual(summarize(state), {
    processed: 5, claims: 3, flagged: 2, rejected: 0,
    byStatus: { SUBMITTED: 2, REVIEWING: 1, APPROVED: 1, DENIED: 1, PAID: 0 },
  });
  assert.equal(eventsForClaim(state, "CL-2048")[0].status, "DENIED");
});

test("duplicate event IDs are idempotent and invalid raw records stay separate", () => {
  const event = createEvent(SAMPLE_EVENTS[0], id(1), "2026-01-15T12:00:00Z");
  const once = acceptEvent(emptyState(), event);
  assert.equal(acceptEvent(once, event), once);
  const rejected = rejectRaw(once, "Malformed Kafka payload");
  assert.equal(summarize(rejected).processed, 1);
  assert.equal(summarize(rejected).rejected, 1);
});

test("invalid claim IDs and values never become accepted events", () => {
  assert.throws(() => createEvent({ ...SAMPLE_EVENTS[0], claimId: "POL-9921" }, id(1)));
  assert.throws(() => createEvent({ ...SAMPLE_EVENTS[0], amountCents: -1 }, id(1)));
  assert.throws(() => createEvent({ ...SAMPLE_EVENTS[0], status: "UNKNOWN" }, id(1)));
});
