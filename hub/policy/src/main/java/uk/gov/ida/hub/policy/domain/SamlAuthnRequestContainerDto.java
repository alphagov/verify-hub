package uk.gov.ida.hub.policy.domain;

import java.util.Optional;

public class SamlAuthnRequestContainerDto {

    private String samlRequest;
    private String relayState;
    private String principalIPAddressAsSeenByHub;


    @SuppressWarnings("unused") //Needed for JAXB
    private SamlAuthnRequestContainerDto() {
    }

    public SamlAuthnRequestContainerDto(String samlRequest, String relayState, String principalIPAddressAsSeenByHub) {
        this.samlRequest = samlRequest;
        this.relayState = relayState;
        this.principalIPAddressAsSeenByHub = principalIPAddressAsSeenByHub;
    }

    public String getSamlRequest() {
        return samlRequest;
    }

    public Optional<String> getRelayState() {
        return Optional.ofNullable(relayState);
    }

    public String getPrincipalIPAddressAsSeenByHub() {return principalIPAddressAsSeenByHub; }
}
