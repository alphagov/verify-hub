package uk.gov.ida.hub.policy.domain;

import java.util.Optional;

public class RequesterErrorResponse {

    private String issuer;
    private Optional<String> errorMessage;
    private String principalIpAddressAsSeenByHub;
    private String analyticsSessionId;
    private String journeyType;

    @SuppressWarnings("unused")//Needed by JAXB
    private RequesterErrorResponse() {
    }

    public RequesterErrorResponse(String issuer, Optional<String> errorMessage, String principalIpAddressAsSeenByHub, String analyticsSessionId, String journeyType) {
        this.issuer = issuer;
        this.errorMessage = errorMessage;
        this.principalIpAddressAsSeenByHub = principalIpAddressAsSeenByHub;
        this.analyticsSessionId = analyticsSessionId;
        this.journeyType = journeyType;
    }

    public String getIssuer() {
        return issuer;
    }

    public Optional<String> getErrorMessage() {
        return errorMessage;
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
