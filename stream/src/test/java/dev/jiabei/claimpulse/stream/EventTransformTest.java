package dev.jiabei.claimpulse.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EventTransformTest {
    private static SparkSession spark;

    @BeforeAll
    static void startSpark() {
        spark = SparkSession.builder().master("local[1]").appName("claimpulse-transform-test")
                .config("spark.ui.enabled", "false").getOrCreate();
    }

    @AfterAll
    static void stopSpark() {
        if (spark != null) spark.stop();
    }

    @Test
    void acceptsOnlyValidEventsAndComputesReviewRule() {
        String valid = """
                {"eventId":"d8bdb872-f95a-4a4d-a1a0-97851b632086","claimId":"CL-2048",\
                "claimType":"HEALTHCARE","status":"DENIED","amountCents":300000,\
                "occurredAt":"2026-01-15T12:00:00Z"}
                """.replace("\\\n", "");
        String invalid = """
                {"eventId":"11c5c00c-0e7c-43b2-8f5b-56dd3e06f849","claimId":"CL-2048",\
                "claimType":"HEALTHCARE","status":"APPROVED","amountCents":-10,\
                "occurredAt":"2026-01-15T12:00:00Z"}
                """.replace("\\\n", "");
        StructType schema = new StructType()
                .add("topic", DataTypes.StringType)
                .add("partition", DataTypes.IntegerType)
                .add("offset", DataTypes.LongType)
                .add("value", DataTypes.BinaryType);
        Dataset<Row> source = spark.createDataFrame(List.of(
                RowFactory.create("claim-events", 0, 1L, valid.getBytes(StandardCharsets.UTF_8)),
                RowFactory.create("claim-events", 0, 2L, invalid.getBytes(StandardCharsets.UTF_8))), schema);

        Dataset<Row> parsed = EventTransform.parse(source);
        List<Row> accepted = EventTransform.accepted(parsed).collectAsList();
        List<Row> rejected = EventTransform.rejected(parsed).collectAsList();
        assertEquals(1, accepted.size());
        assertEquals("CL-2048", accepted.get(0).getAs("claim_id"));
        assertTrue((Boolean) accepted.get(0).getAs("review_flag"));
        assertEquals(1, rejected.size());
        assertEquals(2L, ((Long) rejected.get(0).getAs("kafka_offset")).longValue());
    }
}
