package dev.jiabei.claimpulse.stream;

public record StreamConfig(String kafkaBootstrapServers, String kafkaSecurityProtocol, String topic, String jdbcUrl,
                           String jdbcUser, String jdbcPassword, String checkpointDir)
        implements java.io.Serializable {
    public static StreamConfig fromEnvironment() {
        return new StreamConfig(required("KAFKA_BOOTSTRAP_SERVERS"),
                System.getenv().getOrDefault("KAFKA_SECURITY_PROTOCOL", "PLAINTEXT"),
                System.getenv().getOrDefault("KAFKA_TOPIC", "claim-events"),
                required("JDBC_URL"), required("JDBC_USER"), required("JDBC_PASSWORD"),
                required("CHECKPOINT_DIR"));
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
