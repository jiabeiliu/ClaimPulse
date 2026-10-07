package dev.jiabei.claimpulse.stream;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.api.java.function.VoidFunction2;

public final class ClaimPulseStream {
    private ClaimPulseStream() {}

    public static void main(String[] args) throws Exception {
        StreamConfig config = StreamConfig.fromEnvironment();
        SparkSession spark = SparkSession.builder().appName("ClaimPulse").getOrCreate();
        spark.sparkContext().setLogLevel("WARN");

        Dataset<Row> kafka = spark.readStream().format("kafka")
                .option("kafka.bootstrap.servers", config.kafkaBootstrapServers())
                .option("kafka.security.protocol", config.kafkaSecurityProtocol())
                .option("subscribe", config.topic())
                .option("startingOffsets", "earliest")
                .load();
        Dataset<Row> parsed = EventTransform.parse(kafka);

        StreamingQuery accepted = EventTransform.accepted(parsed)
                .withWatermark("event_time", "10 minutes")
                .dropDuplicates("event_id")
                .writeStream()
                .option("checkpointLocation", config.checkpointDir() + "/accepted")
                .foreachBatch((VoidFunction2<Dataset<Row>, Long>) (batch, id) -> JdbcSink.saveAccepted(batch, config))
                .start();

        StreamingQuery rejected = EventTransform.rejected(parsed)
                .writeStream()
                .option("checkpointLocation", config.checkpointDir() + "/rejected")
                .foreachBatch((VoidFunction2<Dataset<Row>, Long>) (batch, id) -> JdbcSink.saveRejected(batch, config))
                .start();

        // Any failed query terminates the process so the runtime can restart it.
        spark.streams().awaitAnyTermination();
        accepted.stop();
        rejected.stop();
    }
}
