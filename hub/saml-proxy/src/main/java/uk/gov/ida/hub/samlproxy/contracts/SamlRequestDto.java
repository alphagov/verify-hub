package uk.gov.ida.hub.samlproxy.contracts;

public class SamlRequestDto {
    private String samlRequest;
    private String relayState;
    private String principalIpAsSeenByFrontend;

    public SamlRequestDto(String samlRequest, String relayState, final String principalIpAsSeenByFrontend) {
        this.samlRequest = samlRequest;
        this.relayState = relayState;
        this.principalIpAsSeenByFrontend = principalIpAsSeenByFrontend;
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
}
