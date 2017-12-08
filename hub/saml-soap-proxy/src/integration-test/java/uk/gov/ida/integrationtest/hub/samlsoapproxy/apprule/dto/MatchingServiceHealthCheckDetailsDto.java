package uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.dto;

import java.net.URI;

public class MatchingServiceHealthCheckDetailsDto {

    private URI matchingService;
    private String details;
    private String versionNumber;
    private boolean versionSupported;
    private boolean onboarding;

    public MatchingServiceHealthCheckDetailsDto() {}
    
    public MatchingServiceHealthCheckDetailsDto(
            URI matchingService,
            String details,
            String versionNumber,
            boolean versionSupported,
            boolean onboarding) {

        this.matchingService = matchingService;
        this.details = details;
        this.versionNumber = versionNumber;
        this.versionSupported = versionSupported;
        this.onboarding = onboarding;
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

    public boolean isVersionSupported() { return versionSupported; }

    public boolean isOnboarding() {
        return onboarding;
    }
}
