package uk.gov.ida.hub.samlsoapproxy.builders;

import uk.gov.ida.hub.samlsoapproxy.healthcheck.MatchingServiceHealthCheckDetails;

import java.net.URI;

public class MatchingServiceHealthCheckDetailsBuilder {

    private URI matchingServiceUri = URI.create("/default-matching-service-uri");
    private String details = "default-failure-reason";
    private String versionNumber;

    public static MatchingServiceHealthCheckDetailsBuilder aMatchingServiceHealthCheckDetails() {
        return new MatchingServiceHealthCheckDetailsBuilder();
    }

    public MatchingServiceHealthCheckDetails build() {
        return new MatchingServiceHealthCheckDetails(
                matchingServiceUri,
                details,
                versionNumber,
                false,
                false);
    }

    public MatchingServiceHealthCheckDetailsBuilder withDetails(String failureDetails) {
        this.details = failureDetails;
        return this;
    }

    public MatchingServiceHealthCheckDetailsBuilder withMatchingServiceUri(URI matchingServiceUri) {
        this.matchingServiceUri = matchingServiceUri;
        return this;
    }

    public MatchingServiceHealthCheckDetailsBuilder withVersionNumber(String versionNumber) {
        this.versionNumber = versionNumber;
        return this;
    }
}
