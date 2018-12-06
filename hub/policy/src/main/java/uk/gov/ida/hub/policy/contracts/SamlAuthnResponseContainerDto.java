package uk.gov.ida.hub.policy.contracts;


import uk.gov.ida.hub.policy.domain.SessionId;

public class SamlAuthnResponseContainerDto {

    private String samlResponse;
    private SessionId sessionId;
    private String principalIPAddressAsSeenByHub;
    private String analyticsSessionId;
    private String journeyType;

    @SuppressWarnings("unused") //Needed for JAXB
    private SamlAuthnResponseContainerDto() {
    }

    public SamlAuthnResponseContainerDto(String samlResponse, SessionId sessionId, String principalIPAddressAsSeenByHub, String analyticsSessionId, String journeyType) {
        this.samlResponse = samlResponse;
        this.sessionId = sessionId;
        this.principalIPAddressAsSeenByHub = principalIPAddressAsSeenByHub;
        this.analyticsSessionId = analyticsSessionId;
        this.journeyType = journeyType;
    }

    public String getSamlResponse() {
        return samlResponse;
    }

    public SessionId getSessionId() {
        return sessionId;
    }

    public String getPrincipalIPAddressAsSeenByHub() { return principalIPAddressAsSeenByHub; }

    public String getAnalyticsSessionId() {
        return analyticsSessionId;
    }

    public String getJourneyType() {
        return journeyType;
    }
}

