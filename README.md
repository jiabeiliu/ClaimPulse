# ClaimPulse

ClaimPulse is a **Java-first, synthetic insurance-claims event pipeline**. It demonstrates a Spring Boot ingestion/query API, Kafka event transport, Spark Structured Streaming validation and aggregation-ready enrichment, a PostgreSQL serving store, and a small browser dashboard. It is a portfolio engineering demo, **not** a claims adjudication system or an ML model.

## Architecture

```text
POST /api/events (Java 17 / Spring Boot)
      | validated synthetic event + Kafka acknowledgement
      v
Kafka claim-events topic
      | Spark Structured Streaming (Java 17)
      +--> valid event -> review-rule flag -> PostgreSQL claim_events
      +--> invalid event -> PostgreSQL claim_event_rejects
                                              |
GET /api/metrics and /api/claims/{id}/events <- dashboard
```

The API is the only writer to Kafka. Spark parses the Kafka payload against a defined schema, rejects malformed records, and uses a 10-minute watermark plus event-ID deduplication. PostgreSQL primary keys and `ON CONFLICT DO NOTHING` make retried micro-batches idempotent. `review_flag` is an **illustrative deterministic rule** (amount >= $10,000, or denied and amount >= $2,500); it is not a fraud score or insurance decision. Data is generated and fictional; there is no PII.

The modules are `common/` (event contract and rules), `api/` (Spring Boot), and `stream/` (Spark job). `db/init.sql` creates the serving schema. `compose.yml` runs the complete local stack.

## Public interactive demo

**[Open the ClaimPulse demo](https://jiabeiliu.github.io/ClaimPulse/)**. This GitHub Pages site is an interactive, **browser-only simulation** with fictional data. You can replay sample events, enter a synthetic claim event, watch the four pipeline stages, inspect metrics and claim history, and send a malformed synthetic record to the reject path. It does **not** call the Java API, Kafka, Spark, or PostgreSQL and does not persist anything after a reload. The real stack is exercised in [GitHub Actions](https://github.com/jiabeiliu/ClaimPulse/actions) and can be run locally below. The public site is served from `docs/` on the `main` branch.

To test or preview the static demo locally, use Node.js 18+ and run:

```bash
npm run test:demo
python3 -m http.server 8765 --directory docs
```

Then open <http://localhost:8765/>. The Node tests check input validation, review-rule parity, sample metrics, deduplication, and rejection behavior. No npm dependencies or API keys are required.

## Local demo

Requirements: Docker Engine with Compose v2, at least 4 GB available memory, and internet access for the initial image/dependency downloads. The dashboard runs at <http://localhost:8080>. The demo binds only to `127.0.0.1`, uses deliberately local-only PostgreSQL credentials, and enables its unauthenticated event generator; **do not expose this Compose stack publicly**.

```bash
docker compose up --build -d
docker compose ps
curl http://localhost:8080/actuator/health
curl -X POST 'http://localhost:8080/api/demo/generate?count=20'
curl http://localhost:8080/api/metrics
curl http://localhost:8080/api/claims/CL-2000/events
```

Kafka-to-Spark-to-PostgreSQL processing is asynchronous; refresh metrics after a few seconds. The dashboard also has a “Generate 20 events” button. To stop without deleting data, run `docker compose down`. `docker compose down -v` **deletes the local demo database and checkpoints**.

## Tests and build

Java 17 and Maven 3.9+ are needed outside Docker:

```bash
mvn -B verify
```

This runs contract, publishing/authentication, and real local Spark transformation tests, then produces `api/target/api-0.1.0.jar` and `stream/target/stream-0.1.0.jar`. GitHub Actions also runs the static-demo tests and a Docker Compose smoke test. The stream image shades the Kafka connector and PostgreSQL driver into its application JAR, so it does not fetch Maven packages on startup.

## API and security boundaries

`POST /api/events` accepts `claimId`, `claimType` (`HEALTHCARE`, `AUTO`, `HOME`), `status` (`SUBMITTED`, `REVIEWING`, `APPROVED`, `DENIED`, `PAID`), `amountCents`, and optional `occurredAt`. It returns HTTP 202 only after Kafka acknowledges the event. The `eventId` is server-generated. Outside local demo mode, set `CLAIMPULSE_INGEST_TOKEN` and send it as `X-Ingest-Token`. When neither demo mode nor a token is configured, ingestion is disabled. The demo generator exists only when `CLAIMPULSE_DEMO_ENABLED=true`.

This is **not a production authorization system**: read endpoints are open, and the simple ingest token is not per-user authentication. Use fictional data only. A public deployment would need HTTPS, user/role-based access, rate limits, secret management, audit logging, and compliance review.

## AWS deployment blueprint — not deployed

See [`infra/aws/README.md`](infra/aws/README.md) for the configuration mapping to MSK, EMR Serverless, RDS PostgreSQL, ECR, S3 checkpoints, IAM, and private networking. `.env.example` lists application variables. The blueprint intentionally creates **no** AWS resources. AWS services can incur ongoing charges; review cost estimates before provisioning. The project does not claim a hosted AWS demo.

## Known limitations

- The public GitHub Pages demo is a per-browser simulation, not a live backend. The separate local-stack dashboard is functional but intentionally minimal; both use synthetic, resettable data.
- The review rule is illustrative, not trained, validated, or suitable for a real claim decision.
- The local Compose topology has one Kafka broker and one Spark driver. It is not highly available.
- The service has no real customer accounts, claim documents, or production-grade access control.
- Kafka/Spark/PostgreSQL end-to-end correctness depends on the CI Compose smoke test; local Docker runtime verification may differ by machine.
- No cloud infrastructure is provisioned or billed by this repository.
