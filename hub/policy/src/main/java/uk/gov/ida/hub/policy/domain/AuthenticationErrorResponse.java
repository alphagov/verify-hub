package uk.gov.ida.hub.policy.domain;

public class AuthenticationErrorResponse {

    private String issuer;
    private String principalIpAddressAsSeenByHub;
    private String analyticsSessionId;
    private String journeyType;

    @SuppressWarnings("unused")//Needed by JAXB
    private AuthenticationErrorResponse() {
    }

    public AuthenticationErrorResponse(String issuer, String principalIpAddressAsSeenByHub, String analyticsSessionId, String journeyType) {
        this.issuer = issuer;
        this.principalIpAddressAsSeenByHub = principalIpAddressAsSeenByHub;
        this.analyticsSessionId = analyticsSessionId;
        this.journeyType = journeyType;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getPrincipalIpAddressAsSeenByHub() {
        return principalIpAddressAsSeenByHub;
    }

    public String getAnalyticsSessionId() {
        return analyticsSessionId;
    }

    public String getJourneyType() {
        return journeyType;
    }
}
