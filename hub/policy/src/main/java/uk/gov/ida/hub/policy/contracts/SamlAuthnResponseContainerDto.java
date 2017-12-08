package uk.gov.ida.hub.policy.contracts;


import uk.gov.ida.hub.policy.domain.SessionId;

public class SamlAuthnResponseContainerDto {

    private String samlResponse;
    private SessionId sessionId;
    private String principalIPAddressAsSeenByHub;

    @SuppressWarnings("unused") //Needed for JAXB
    private SamlAuthnResponseContainerDto() {
    }

    public SamlAuthnResponseContainerDto(String samlResponse, SessionId sessionId, String principalIPAddressAsSeenByHub) {
        this.samlResponse = samlResponse;
        this.sessionId = sessionId;
        this.principalIPAddressAsSeenByHub = principalIPAddressAsSeenByHub;
    }

    public String getSamlResponse() {
        return samlResponse;
    }

    public SessionId getSessionId() {
        return sessionId;
    }

    public String getPrincipalIPAddressAsSeenByHub() { return principalIPAddressAsSeenByHub; }
}

