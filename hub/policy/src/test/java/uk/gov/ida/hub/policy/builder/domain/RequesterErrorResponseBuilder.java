package uk.gov.ida.hub.policy.builder.domain;

import uk.gov.ida.hub.policy.domain.RequesterErrorResponse;

import java.util.Optional;

public class RequesterErrorResponseBuilder {

    private String issuer = "issuer";
    private Optional<String> errorMessage = Optional.empty();
    private String principalIpAddressAsSeenByHub = "principal ip address as seen by hub";
    private String analyticsSessionId = "some-analytics-session-id";
    private String journeyType = "some-journey-type";

    public static RequesterErrorResponseBuilder aRequesterErrorResponse() {
        return new RequesterErrorResponseBuilder();
    }

    public RequesterErrorResponse build() {
        return new RequesterErrorResponse(issuer, errorMessage, principalIpAddressAsSeenByHub, analyticsSessionId, journeyType);
    }

    public RequesterErrorResponseBuilder withIssuerId(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public RequesterErrorResponseBuilder withErrorMessage(String errorMessage) {
        this.errorMessage = Optional.ofNullable(errorMessage);
        return this;
    }

    public RequesterErrorResponseBuilder withPrincipalIpAddressAsSeenByHub(String principalIpAddressAsSeenByHub) {
        this.principalIpAddressAsSeenByHub = principalIpAddressAsSeenByHub;
        return this;
    }

    public RequesterErrorResponseBuilder withAnalyticsSessionId(String analyticsSessionId) {
        this.analyticsSessionId = analyticsSessionId;
        return this;
    }

    public RequesterErrorResponseBuilder withJourneyType(String journeyType) {
        this.journeyType = journeyType;
        return this;
    }

}
