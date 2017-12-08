package uk.gov.ida.hub.samlsoapproxy.healthcheck;

public final class MatchingServiceHealthCheckResult {
    private boolean healthy;
    private MatchingServiceHealthCheckDetails details;

    @SuppressWarnings("unused") // required for JAX B
    private MatchingServiceHealthCheckResult() {
    }

    private MatchingServiceHealthCheckResult(boolean healthy, MatchingServiceHealthCheckDetails message) {
        this.healthy = healthy;
        this.details = message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MatchingServiceHealthCheckResult that = (MatchingServiceHealthCheckResult) o;

        if (healthy != that.healthy) {
            return false;
        }
        if (!details.equals(that.details)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = healthy ? 1 : 0;
        result = 31 * result + details.hashCode();
        return result;
    }

    public boolean isHealthy() {
        return healthy;
    }

    public MatchingServiceHealthCheckDetails getDetails() {
        return details;
    }

    public static MatchingServiceHealthCheckResult healthy(MatchingServiceHealthCheckDetails healthCheckDescription) {
        return new MatchingServiceHealthCheckResult(true, healthCheckDescription);
    }

    public static MatchingServiceHealthCheckResult unhealthy(MatchingServiceHealthCheckDetails healthCheckDescription) {
        return new MatchingServiceHealthCheckResult(false, healthCheckDescription);
    }
}
