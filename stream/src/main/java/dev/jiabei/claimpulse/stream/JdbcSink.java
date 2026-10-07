package dev.jiabei.claimpulse.stream;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.apache.spark.api.java.function.ForeachPartitionFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

/** Idempotent inserts make Spark micro-batch retries safe for this demo sink. */
public final class JdbcSink {
    private JdbcSink() {}

    public static void saveAccepted(Dataset<Row> batch, StreamConfig config) {
        batch.foreachPartition((ForeachPartitionFunction<Row>) rows -> {
            try (Connection connection = connect(config);
                 PreparedStatement insert = connection.prepareStatement("""
                         INSERT INTO claim_events(event_id,claim_id,claim_type,status,amount_cents,event_time,review_flag)
                         VALUES (?,?,?,?,?,?,?) ON CONFLICT (event_id) DO NOTHING
                         """)) {
                connection.setAutoCommit(false);
                while (rows.hasNext()) {
                    Row row = rows.next();
                    insert.setString(1, row.getAs("event_id"));
                    insert.setString(2, row.getAs("claim_id"));
                    insert.setString(3, row.getAs("claim_type"));
                    insert.setString(4, row.getAs("status"));
                    insert.setLong(5, row.getAs("amount_cents"));
                    insert.setTimestamp(6, row.getAs("event_time"));
                    insert.setBoolean(7, row.getAs("review_flag"));
                    insert.addBatch();
                }
                insert.executeBatch();
                connection.commit();
            }
        });
    }

    public static void saveRejected(Dataset<Row> batch, StreamConfig config) {
        batch.foreachPartition((ForeachPartitionFunction<Row>) rows -> {
            try (Connection connection = connect(config);
                 PreparedStatement insert = connection.prepareStatement("""
                         INSERT INTO claim_event_rejects(topic,kafka_partition,kafka_offset,raw_json,reason)
                         VALUES (?,?,?,?,?) ON CONFLICT (topic,kafka_partition,kafka_offset) DO NOTHING
                         """)) {
                connection.setAutoCommit(false);
                while (rows.hasNext()) {
                    Row row = rows.next();
                    insert.setString(1, row.getAs("topic"));
                    insert.setInt(2, row.getAs("kafka_partition"));
                    insert.setLong(3, row.getAs("kafka_offset"));
                    insert.setString(4, row.getAs("raw_json"));
                    insert.setString(5, row.getAs("reason"));
                    insert.addBatch();
                }
                insert.executeBatch();
                connection.commit();
            }
        });
    }

    private static Connection connect(StreamConfig config) throws SQLException {
        return DriverManager.getConnection(config.jdbcUrl(), config.jdbcUser(), config.jdbcPassword());
    }
}
