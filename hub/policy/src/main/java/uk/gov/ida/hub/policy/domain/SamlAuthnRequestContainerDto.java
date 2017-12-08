package uk.gov.ida.hub.policy.domain;

import com.google.common.base.Optional;

public class SamlAuthnRequestContainerDto {

    private String samlRequest;
    private Optional<String> relayState = Optional.absent();
    private String principalIPAddressAsSeenByHub;


    @SuppressWarnings("unused") //Needed for JAXB
    private SamlAuthnRequestContainerDto() {
    }

    public SamlAuthnRequestContainerDto(String samlRequest, Optional<String> relayState, String principalIPAddressAsSeenByHub) {
        this.samlRequest = samlRequest;
        this.relayState = relayState;
        this.principalIPAddressAsSeenByHub = principalIPAddressAsSeenByHub;
    }

    public String getSamlRequest() {
        return samlRequest;
    }

    public Optional<String> getRelayState() {
        return relayState;
    }

    public String getPrincipalIPAddressAsSeenByHub() {return principalIPAddressAsSeenByHub; }
}
