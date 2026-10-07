package dev.jiabei.claimpulse.stream;

import static org.apache.spark.sql.functions.*;

import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

/** One transformation is used by the live streaming query and batch fixture tests. */
public final class EventTransform {
    private static final StructType CONTRACT = new StructType()
            .add("eventId", DataTypes.StringType)
            .add("claimId", DataTypes.StringType)
            .add("claimType", DataTypes.StringType)
            .add("status", DataTypes.StringType)
            .add("amountCents", DataTypes.LongType)
            .add("occurredAt", DataTypes.StringType);

    private EventTransform() {}

    public static Dataset<Row> parse(Dataset<Row> source) {
        Column eventId = col("payload.eventId");
        Column claimId = col("payload.claimId");
        Column type = col("payload.claimType");
        Column status = col("payload.status");
        Column amount = col("payload.amountCents");
        Column eventTime = col("event_time");
        Column valid = eventId.rlike("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
                .and(claimId.rlike("CL-[0-9]{4,8}"))
                .and(type.isin("HEALTHCARE", "AUTO", "HOME"))
                .and(status.isin("SUBMITTED", "REVIEWING", "APPROVED", "DENIED", "PAID"))
                .and(amount.gt(0)).and(amount.leq(10_000_000_000L))
                .and(eventTime.isNotNull());
        return source
                .select(col("topic"), col("partition").alias("kafka_partition"),
                        col("offset").alias("kafka_offset"),
                        col("value").cast("string").alias("raw_json"))
                .withColumn("payload", from_json(col("raw_json"), CONTRACT))
                .withColumn("event_time", to_timestamp(col("payload.occurredAt")))
                .withColumn("is_valid", coalesce(valid, lit(false)));
    }

    public static Dataset<Row> accepted(Dataset<Row> parsed) {
        Column amount = col("payload.amountCents");
        Column status = col("payload.status");
        return parsed.filter(col("is_valid"))
                .select(col("payload.eventId").alias("event_id"),
                        col("payload.claimId").alias("claim_id"),
                        col("payload.claimType").alias("claim_type"),
                        status.alias("status"), amount.alias("amount_cents"),
                        col("event_time"),
                        amount.geq(1_000_000L).or(status.equalTo("DENIED").and(amount.geq(250_000L)))
                                .alias("review_flag"));
    }

    public static Dataset<Row> rejected(Dataset<Row> parsed) {
        return parsed.filter(col("is_valid").equalTo(false))
                .select(col("topic"), col("kafka_partition"), col("kafka_offset"),
                        col("raw_json"), lit("Invalid JSON or event contract").alias("reason"));
    }
}
