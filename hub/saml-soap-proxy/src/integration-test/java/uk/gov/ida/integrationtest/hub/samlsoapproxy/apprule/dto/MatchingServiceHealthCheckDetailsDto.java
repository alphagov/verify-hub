package uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.dto;

import java.net.URI;

public class MatchingServiceHealthCheckDetailsDto {

    private URI matchingService;
    private String details;
    private String versionNumber;
    private boolean versionSupported;
    private boolean onboarding;
    private String eidasEnabled;
    private String shouldSignWithSha1;

    public MatchingServiceHealthCheckDetailsDto(){}
    
    public MatchingServiceHealthCheckDetailsDto(
            URI matchingService,
            String details,
            String versionNumber,
            boolean versionSupported,
            boolean onboarding,
            String isEidasEnabled,
            String shouldSignWithSha1) {

        this.matchingService = matchingService;
        this.details = details;
        this.versionNumber = versionNumber;
        this.versionSupported = versionSupported;
        this.onboarding = onboarding;
        this.eidasEnabled = isEidasEnabled;
        this.shouldSignWithSha1 = shouldSignWithSha1;
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

    public String getEidasEnabled() {
        return eidasEnabled;
    }

    public String getShouldSignWithSha1(){
        return shouldSignWithSha1;
    }
}
