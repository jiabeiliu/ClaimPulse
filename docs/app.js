import {
  SAMPLE_EVENTS, STATUSES, acceptEvent, createEvent, dollarsToCents,
  emptyState, eventsForClaim, rejectRaw, summarize,
} from "./simulator.js";

const $ = (id) => document.getElementById(id);
const stageIds = ["api", "kafka", "spark", "postgres"];
const dollars = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });
const seededId = (number) => `00000000-0000-4000-8000-${String(number).padStart(12, "0")}`;
const pause = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds));

let state = emptyState();
let busy = false;
let filter = "CL-2048";

for (const [index, input] of SAMPLE_EVENTS.entries()) {
  state = acceptEvent(state, createEvent(input, seededId(index + 1), "2026-01-15T12:00:00Z"));
}

function setBusy(value) {
  busy = value;
  for (const id of ["run-sample", "send-event", "inject-invalid", "reset"]) $(id).disabled = value;
}

function clearStages() {
  for (const id of stageIds) {
    const stage = $("stage-" + id);
    stage.classList.remove("active", "done", "error");
    stage.querySelector(".stage-state").textContent = "READY";
  }
}

function markStage(id, stateLabel, className) {
  const stage = $("stage-" + id);
  stage.classList.remove("active", "done", "error");
  if (className) stage.classList.add(className);
  stage.querySelector(".stage-state").textContent = stateLabel;
}

function addActivity(message, tone = "normal") {
  const item = document.createElement("li");
  const marker = document.createElement("span");
  marker.className = "activity-marker" + (tone === "normal" ? "" : " " + tone);
  marker.textContent = tone === "error" ? "!" : tone === "warning" ? "↳" : "✓";
  marker.setAttribute("aria-hidden", "true");
  const content = document.createElement("span");
  content.textContent = message;
  const time = document.createElement("time");
  time.dateTime = new Date().toISOString();
  time.textContent = new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
  content.append(time);
  item.append(marker, content);
  $("activity-list").prepend(item);
  while ($("activity-list").children.length > 8) $("activity-list").lastElementChild.remove();
}

function setMessage(message, error = false) {
  $("form-message").textContent = message;
  $("form-message").classList.toggle("error", error);
}

function renderMetrics() {
  const summary = summarize(state);
  $("metric-events").textContent = summary.processed;
  $("metric-claims").textContent = summary.claims;
  $("metric-flags").textContent = summary.flagged;
  $("metric-rejected").textContent = summary.rejected;
  return summary;
}

function badge(text, className) {
  const element = document.createElement("span");
  element.className = className;
  element.textContent = text;
  return element;
}

function renderEvents() {
  const body = $("events-body");
  body.replaceChildren();
  const events = filter ? eventsForClaim(state, filter) : [...state.events].reverse();
  for (const event of events.slice(0, 12)) {
    const row = document.createElement("tr");
    for (const value of [event.claimId, event.claimType]) {
      const cell = document.createElement("td");
      cell.textContent = value;
      row.append(cell);
    }
    const status = document.createElement("td");
    status.append(badge(event.status, "status-badge " + event.status.toLowerCase()));
    const amount = document.createElement("td");
    amount.textContent = dollars.format(event.amountCents / 100);
    const rule = document.createElement("td");
    rule.append(badge(event.reviewFlag ? "FLAGGED" : "CLEAR", "rule-badge" + (event.reviewFlag ? "" : " clear")));
    row.append(status, amount, rule);
    body.append(row);
  }
  $("events-empty").hidden = events.length > 0;
}

function renderChart(summary) {
  const chart = $("status-chart");
  chart.replaceChildren();
  const maximum = Math.max(1, ...Object.values(summary.byStatus));
  for (const status of STATUSES) {
    const wrapper = document.createElement("div");
    const label = document.createElement("div");
    label.className = "chart-label";
    const name = document.createElement("span");
    name.textContent = status;
    const count = document.createElement("span");
    count.textContent = summary.byStatus[status];
    label.append(name, count);
    const track = document.createElement("div");
    track.className = "chart-track";
    const fill = document.createElement("div");
    fill.className = "chart-fill";
    fill.style.width = `${summary.byStatus[status] / maximum * 100}%`;
    track.append(fill);
    wrapper.append(label, track);
    chart.append(wrapper);
  }
}

function render() {
  const summary = renderMetrics();
  renderEvents();
  renderChart(summary);
}

async function step(id, message) {
  markStage(id, "PROCESSING", "active");
  addActivity(message);
  await pause(260);
  markStage(id, "COMPLETE", "done");
}

async function processEvent(event) {
  clearStages();
  await step("api", `${event.claimId}: validated by the simulated Spring Boot API.`);
  await step("kafka", `${event.claimId}: published to the simulated claim-events topic.`);
  await step("spark", `${event.claimId}: parsed, deduplicated, and evaluated by the review rule.`);
  await step("postgres", `${event.claimId}: inserted into the simulated serving view.`);
  state = acceptEvent(state, event);
  filter = event.claimId;
  $("filter-claim").value = filter;
  render();
  addActivity(`${event.claimId}: ${event.status.toLowerCase()} event ready to query${event.reviewFlag ? "; review rule flagged" : ""}.`, event.reviewFlag ? "warning" : "normal");
}

$("event-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  if (busy) return;
  let claimEvent;
  try {
    claimEvent = createEvent({
      claimId: $("claim-id").value.trim().toUpperCase(),
      claimType: $("claim-type").value,
      status: $("claim-status").value,
      amountCents: dollarsToCents($("claim-amount").value),
    });
  } catch (error) {
    clearStages();
    markStage("api", "REJECTED", "error");
    setMessage(`${error.message} No Kafka event was created.`, true);
    addActivity(`API validation stopped the input: ${error.message}`, "error");
    return;
  }
  setBusy(true);
  setMessage("Simulating event flow. No data leaves this page.");
  try {
    await processEvent(claimEvent);
    setMessage(`${claimEvent.claimId} processed in this browser-only simulation.`);
  } finally {
    setBusy(false);
  }
});

$("run-sample").addEventListener("click", async () => {
  if (busy) return;
  setBusy(true);
  state = emptyState();
  filter = "";
  $("filter-claim").value = "";
  $("activity-list").replaceChildren();
  clearStages();
  render();
  setMessage("Replaying five fictional events in your browser.");
  try {
    for (const input of SAMPLE_EVENTS) await processEvent(createEvent(input));
    filter = "CL-2048";
    $("filter-claim").value = filter;
    render();
    setMessage("Sample replay complete: five accepted events across three fictional claims.");
  } finally {
    setBusy(false);
  }
});

$("inject-invalid").addEventListener("click", async () => {
  if (busy) return;
  setBusy(true);
  clearStages();
  setMessage("Simulating a malformed record written directly to Kafka.");
  try {
    await step("kafka", "Malformed synthetic payload entered the simulated Kafka topic (not through the API).");
    markStage("spark", "REJECTED", "error");
    await pause(260);
    state = rejectRaw(state, "Invalid JSON or event contract");
    render();
    addActivity("Spark rejected the raw Kafka record into the simulated rejects table.", "error");
    setMessage("Rejected record counted separately; accepted claim metrics are unchanged.");
  } finally {
    setBusy(false);
  }
});

$("reset").addEventListener("click", () => {
  if (busy) return;
  state = emptyState();
  filter = "";
  $("filter-claim").value = "";
  $("activity-list").replaceChildren();
  clearStages();
  render();
  setMessage("Demo reset. You can replay sample events or create a fictional event.");
  addActivity("Browser-only simulation reset.");
});

function applyFilter() {
  const value = $("filter-claim").value.trim().toUpperCase();
  if (value && !/^CL-[0-9]{4,8}$/.test(value)) {
    setMessage("Filter by a fictional claim ID such as CL-2048.", true);
    return;
  }
  filter = value;
  $("filter-claim").value = value;
  renderEvents();
  setMessage(value ? `Showing events for ${value}.` : "Showing all accepted events.");
}

$("filter-button").addEventListener("click", applyFilter);
$("filter-claim").addEventListener("keydown", (event) => {
  if (event.key === "Enter") applyFilter();
});
$("show-all").addEventListener("click", () => {
  filter = "";
  $("filter-claim").value = "";
  renderEvents();
  setMessage("Showing all accepted events.");
});

render();
addActivity("Five fictional sample events loaded into this browser snapshot.");
