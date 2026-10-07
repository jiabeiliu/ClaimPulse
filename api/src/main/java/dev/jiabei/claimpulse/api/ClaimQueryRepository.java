package dev.jiabei.claimpulse.api;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ClaimQueryRepository {
    private final JdbcTemplate jdbc;

    public ClaimQueryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> events(String claimId) {
        return jdbc.queryForList("""
                SELECT event_id, claim_id, claim_type, status, amount_cents, event_time, review_flag
                FROM claim_events WHERE claim_id = ? ORDER BY event_time DESC, event_id DESC LIMIT 100
                """, claimId);
    }

    public Map<String, Object> metrics() {
        Map<String, Object> totals = jdbc.queryForMap("""
                SELECT count(*) AS total_events,
                       count(*) FILTER (WHERE review_flag) AS flagged_events,
                       count(DISTINCT claim_id) AS distinct_claims
                FROM claim_events
                """);
        List<Map<String, Object>> byStatus = jdbc.queryForList("""
                SELECT status, count(*) AS total FROM claim_events GROUP BY status ORDER BY status
                """);
        List<Map<String, Object>> byType = jdbc.queryForList("""
                SELECT claim_type, count(*) AS total FROM claim_events GROUP BY claim_type ORDER BY claim_type
                """);
        return Map.of("totals", totals, "byStatus", byStatus, "byType", byType);
    }
}
