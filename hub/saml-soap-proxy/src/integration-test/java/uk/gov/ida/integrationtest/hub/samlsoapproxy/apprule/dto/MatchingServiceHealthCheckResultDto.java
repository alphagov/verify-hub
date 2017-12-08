package uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.dto;

public final class MatchingServiceHealthCheckResultDto {

    private boolean healthy;
    private MatchingServiceHealthCheckDetailsDto details;

    public MatchingServiceHealthCheckResultDto() {}

    public MatchingServiceHealthCheckResultDto(boolean healthy, MatchingServiceHealthCheckDetailsDto message) {
        this.healthy = healthy;
        this.details = message;
    }

    public boolean isHealthy() {
        return healthy;
    }

    public MatchingServiceHealthCheckDetailsDto getDetails() {
        return details;
    }

}
