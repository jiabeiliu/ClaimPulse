# AWS deployment blueprint (configuration only)

This document maps the runnable local stack to AWS without creating resources. **Nothing here has been deployed or load-tested on AWS.** Do not provision the services without a budget and cost review: MSK, EMR Serverless, RDS, networking, and data transfer can all cost money.

| Local component | AWS counterpart | Notes |
|---|---|---|
| `api` container | ECR image on ECS Fargate behind HTTPS load balancer | Keep `CLAIMPULSE_DEMO_ENABLED=false`; inject the ingest token from Secrets Manager. |
| Kafka broker | Amazon MSK in private subnets | Use TLS broker endpoints and `KAFKA_SECURITY_PROTOCOL=SSL`. Do not expose brokers publicly. |
| Spark stream | EMR Serverless Spark application, release 7.14.0 | This release uses Spark 3.5.8, matching this project's `spark.version`. |
| PostgreSQL | RDS PostgreSQL in private subnets | Initialize with `db/init.sql`; require TLS using a JDBC URL with `sslmode=require`. |
| `/checkpoint` volume | Private, versioned S3 bucket | Set `CHECKPOINT_DIR=s3://YOUR_BUCKET/claimpulse/checkpoints`. Restrict job-role access. |

## Runtime mapping

The API container requires `JDBC_URL`, `JDBC_USER`, `JDBC_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_SECURITY_PROTOCOL=SSL`, `KAFKA_TOPIC=claim-events`, `CLAIMPULSE_INGEST_TOKEN`, and `CLAIMPULSE_DEMO_ENABLED=false`.

The Spark driver requires `JDBC_URL`, `JDBC_USER`, `JDBC_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_SECURITY_PROTOCOL=SSL`, `KAFKA_TOPIC=claim-events`, and `CHECKPOINT_DIR`. The job loads these at startup; its configuration is serialized to Spark executors. Upload `stream/target/stream-0.1.0.jar` to a private S3 artifact bucket and submit `dev.jiabei.claimpulse.stream.ClaimPulseStream` as the main class. Set streaming mode, execution role, VPC/subnet/security groups, and driver environment variables in the EMR Serverless job configuration. The JDBC URL needs to reach RDS from workers as well as driver. Keep secrets out of source control, build arguments, job logs, and shell history; use Secrets Manager or secure runtime injection.

EMR Serverless streaming jobs support Kafka/MSK sources. Before deployment, validate connector/classpath behavior on the selected EMR release and set appropriate streaming job limits and restart policy. The local Spark JAR packages the Kafka connector and PostgreSQL JDBC driver; Spark core libraries are provided by EMR.

## Deployment sequence (manual approval required)

1. Estimate and approve cost, choose an AWS region, and configure a private VPC with appropriate routing and security groups.
2. Provision MSK, RDS, S3 buckets, ECR, and an EMR Serverless Spark application. Create least-privilege IAM roles for ECS and EMR; use OIDC for any GitHub Actions deployment workflow rather than long-lived AWS keys.
3. Apply `db/init.sql` to RDS. Create the `claim-events` topic. Confirm TLS connections from ECS and EMR to MSK and RDS.
4. Run `mvn -B verify`; build/push `api/Dockerfile` to ECR and upload the shaded stream JAR to S3.
5. Deploy the API with demo mode disabled. Submit the EMR Serverless streaming job with a durable S3 checkpoint. Ingest a fictional event and verify it appears in RDS and the API.
6. Configure observability, alerts, retention, access controls, backup/restore, and shutdown procedures before describing this as production-ready.

This is a blueprint, not turnkey Terraform or a claim of a live deployment. The configuration deliberately does not contain account IDs, credentials, domains, or cloud resources.
