package uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.dto;

import java.util.List;

public class AggregatedMatchingServicesHealthCheckResultDto {

    private List<MatchingServiceHealthCheckResultDto> results;
    private boolean healthy;

    public AggregatedMatchingServicesHealthCheckResultDto() {}

    public AggregatedMatchingServicesHealthCheckResultDto(List<MatchingServiceHealthCheckResultDto> results, boolean healthy) {
        this.results = results;

        this.healthy = healthy;
    }

    public boolean isHealthy() {
        return healthy;
    }

    public List<MatchingServiceHealthCheckResultDto> getResults() {
        return results;
    }
}
