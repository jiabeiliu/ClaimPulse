package dev.jiabei.claimpulse.api;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api")
public class QueryController {
    private final ClaimQueryRepository queries;

    public QueryController(ClaimQueryRepository queries) {
        this.queries = queries;
    }

    @GetMapping("/metrics")
    public Map<String, Object> metrics() {
        return queries.metrics();
    }

    @GetMapping("/claims/{claimId}/events")
    public List<Map<String, Object>> events(@PathVariable String claimId) {
        if (!claimId.matches("CL-[0-9]{4,8}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid claim ID");
        }
        return queries.events(claimId);
    }
}
