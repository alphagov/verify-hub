package uk.gov.ida.hub.samlsoapproxy.healthcheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AggregatedMatchingServicesHealthCheckResult {

    private final List<MatchingServiceHealthCheckResult> results;

    public AggregatedMatchingServicesHealthCheckResult() {
        results = new ArrayList<>();
    }

    public void addResult(MatchingServiceHealthCheckResult result) {
        results.add(result);
    }

    public boolean isHealthy() {
        return results.stream()
                .filter(result -> !result.getDetails().isOnboarding())
                .anyMatch(MatchingServiceHealthCheckResult::isHealthy);
    }

    public List<MatchingServiceHealthCheckResult> getResults() {
        return Collections.unmodifiableList(results);
    }
}
