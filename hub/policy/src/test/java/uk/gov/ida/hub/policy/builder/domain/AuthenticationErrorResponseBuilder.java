package uk.gov.ida.hub.policy.builder.domain;

import uk.gov.ida.hub.policy.domain.AuthenticationErrorResponse;

public class AuthenticationErrorResponseBuilder {

    private String issuer = "issuer";
    private String principalIpAddressAsSeenByHub = "principal-ip-address";
    private String analyticsSessionId = "some-analytics-session-id";
    private String journeyType = "some-journey-type";


    public static AuthenticationErrorResponseBuilder anAuthenticationErrorResponse() {
        return new AuthenticationErrorResponseBuilder();
    }

    public AuthenticationErrorResponse build() {
        return new AuthenticationErrorResponse(issuer, principalIpAddressAsSeenByHub, analyticsSessionId, journeyType);
    }

    public AuthenticationErrorResponseBuilder withIssuerId(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public AuthenticationErrorResponseBuilder withPrincipalIpAddressAsSeenByHub(String principalIpAddressAsSeenByHub) {
        this.principalIpAddressAsSeenByHub = principalIpAddressAsSeenByHub;
        return this;
    }

    public AuthenticationErrorResponseBuilder withAnalyticsSessionId(String analyticsSessionId) {
        this.analyticsSessionId = analyticsSessionId;
        return this;
    }

    public AuthenticationErrorResponseBuilder withJourneyType(String journeyType) {
        this.journeyType = journeyType;
        return this;
    }
}
