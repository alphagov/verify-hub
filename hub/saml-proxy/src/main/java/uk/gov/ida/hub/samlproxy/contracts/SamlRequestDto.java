package uk.gov.ida.hub.samlproxy.contracts;

public class SamlRequestDto {
    private String samlRequest;
    private String relayState;
    private String principalIpAsSeenByFrontend;
    private String analyticsSessionId;
    private String journeyType;

    public SamlRequestDto(String samlRequest, String relayState, String principalIpAsSeenByFrontend, String analyticsSessionId, String journeyType) {
        this.samlRequest = samlRequest;
        this.relayState = relayState;
        this.principalIpAsSeenByFrontend = principalIpAsSeenByFrontend;
        this.analyticsSessionId = analyticsSessionId;
        this.journeyType = journeyType;
    }

    //Needed for JAXB
    public SamlRequestDto() {}

    public String getSamlRequest() {
        return samlRequest;
    }

    public String getRelayState() {
        return relayState;
    }

    public String getPrincipalIpAsSeenByFrontend() {
        return principalIpAsSeenByFrontend;
    }

    public String getAnalyticsSessionId() {
        return analyticsSessionId;
    }

    public String getJourneyType() {
        return journeyType;
    }
}
