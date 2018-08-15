package uk.gov.ida.hub.samlsoapproxy.healthcheck;

import java.net.URI;

public class MatchingServiceHealthCheckDetails {

    private final URI matchingService;
    private final String details;
    private final String versionNumber;
    private final boolean isVersionSupported;
    private final boolean isOnboarding;
    private final String eidasEnabled;
    private final String shouldSignWithSha1;

    public MatchingServiceHealthCheckDetails(
            final URI matchingService,
            final String details,
            final String versionNumber,
            final boolean isVersionSupported,
            final boolean isOnboarding,
            final String eidasEnabled,
            final String shouldSignWithSha1) {

        this.matchingService = matchingService;
        this.details = details;
        this.versionNumber = versionNumber;
        this.isVersionSupported = isVersionSupported;
        this.isOnboarding = isOnboarding;
        this.eidasEnabled = eidasEnabled;
        this.shouldSignWithSha1 = shouldSignWithSha1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MatchingServiceHealthCheckDetails that = (MatchingServiceHealthCheckDetails) o;

        if (!details.equals(that.details)) {
            return false;
        }
        if (!matchingService.equals(that.matchingService)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = matchingService.hashCode();
        result = 31 * result + details.hashCode();
        return result;
    }

    public URI getMatchingService() {
        return matchingService;
    }

    public String getDetails() {
        return details;
    }

    public String getVersionNumber() {
        return versionNumber;
    }

    public boolean isVersionSupported() { return isVersionSupported ; }

    public boolean isOnboarding() {
        return isOnboarding;
    }

    public String getEidasEnabled() {
        return eidasEnabled;
    }

    public String getShouldSignWithSha1() {
        return shouldSignWithSha1;
    }
}
