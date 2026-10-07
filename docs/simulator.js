// Browser-only portfolio simulation. The production implementation is in common/, api/, and stream/.
export const CLAIM_TYPES = Object.freeze(["HEALTHCARE", "AUTO", "HOME"]);
export const STATUSES = Object.freeze(["SUBMITTED", "REVIEWING", "APPROVED", "DENIED", "PAID"]);
export const HIGH_AMOUNT_CENTS = 1_000_000;
export const DENIED_AMOUNT_CENTS = 250_000;
export const MAX_AMOUNT_CENTS = 10_000_000_000;

export const SAMPLE_EVENTS = Object.freeze([
  { claimId: "CL-2048", claimType: "HEALTHCARE", status: "SUBMITTED", amountCents: 320_000 },
  { claimId: "CL-2049", claimType: "AUTO", status: "REVIEWING", amountCents: 180_000 },
  { claimId: "CL-2050", claimType: "HOME", status: "SUBMITTED", amountCents: 1_250_000 },
  { claimId: "CL-2048", claimType: "HEALTHCARE", status: "DENIED", amountCents: 320_000 },
  { claimId: "CL-2049", claimType: "AUTO", status: "APPROVED", amountCents: 180_000 },
]);

export function dollarsToCents(value) {
  const normalized = String(value).trim();
  if (!/^(?:0|[1-9]\d*)(?:\.\d{1,2})?$/.test(normalized)) {
    throw new Error("Enter a non-negative amount with at most two decimal places.");
  }
  const [dollars, fraction = ""] = normalized.split(".");
  const cents = Number(dollars) * 100 + Number(fraction.padEnd(2, "0"));
  if (!Number.isSafeInteger(cents) || cents < 1 || cents > MAX_AMOUNT_CENTS) {
    throw new Error("Amount must be between $0.01 and $100,000,000.00.");
  }
  return cents;
}

export function flagForReview(status, amountCents) {
  return amountCents >= HIGH_AMOUNT_CENTS ||
    (status === "DENIED" && amountCents >= DENIED_AMOUNT_CENTS);
}

export function createEvent(input, eventId = crypto.randomUUID(), occurredAt = new Date().toISOString()) {
  if (!/^CL-[0-9]{4,8}$/.test(input.claimId)) {
    throw new Error("Claim ID must look like CL-2048 (4–8 digits).");
  }
  if (!CLAIM_TYPES.includes(input.claimType)) throw new Error("Choose a supported claim type.");
  if (!STATUSES.includes(input.status)) throw new Error("Choose a supported status.");
  if (!Number.isSafeInteger(input.amountCents) || input.amountCents < 1 || input.amountCents > MAX_AMOUNT_CENTS) {
    throw new Error("Amount is outside the supported range.");
  }
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(eventId)) {
    throw new Error("Event ID must be a UUID.");
  }
  if (Number.isNaN(Date.parse(occurredAt))) throw new Error("Event timestamp is invalid.");
  return Object.freeze({
    eventId, claimId: input.claimId, claimType: input.claimType,
    status: input.status, amountCents: input.amountCents, occurredAt,
    reviewFlag: flagForReview(input.status, input.amountCents),
  });
}

export function emptyState() {
  return { events: [], rejects: [] };
}

export function acceptEvent(state, event) {
  if (state.events.some((current) => current.eventId === event.eventId)) return state;
  return { events: [...state.events, event], rejects: state.rejects };
}

export function rejectRaw(state, reason) {
  return { events: state.events, rejects: [...state.rejects, { reason }] };
}

export function summarize(state) {
  const byStatus = Object.fromEntries(STATUSES.map((status) => [status, 0]));
  for (const event of state.events) byStatus[event.status] += 1;
  return {
    processed: state.events.length,
    claims: new Set(state.events.map((event) => event.claimId)).size,
    flagged: state.events.filter((event) => event.reviewFlag).length,
    rejected: state.rejects.length,
    byStatus,
  };
}

export function eventsForClaim(state, claimId) {
  return state.events.filter((event) => event.claimId === claimId).reverse();
}
