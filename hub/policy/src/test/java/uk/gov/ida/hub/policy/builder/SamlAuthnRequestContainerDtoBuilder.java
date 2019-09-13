package uk.gov.ida.hub.policy.builder;

import uk.gov.ida.hub.policy.domain.SamlAuthnRequestContainerDto;
import uk.gov.ida.shared.utils.string.StringEncoding;


public class SamlAuthnRequestContainerDtoBuilder {

    private String samlRequest = StringEncoding.toBase64Encoded("blah");
    private String relayState;
    private String principalIPAddressAsSeenByHub = "NOT SET IN BUILDER";

    public static SamlAuthnRequestContainerDtoBuilder aSamlAuthnRequestContainerDto() {
        return new SamlAuthnRequestContainerDtoBuilder();
    }

    public SamlAuthnRequestContainerDto build() {
        return new SamlAuthnRequestContainerDto(
                samlRequest,
                relayState,
                principalIPAddressAsSeenByHub);
    }

    public SamlAuthnRequestContainerDtoBuilder withSamlRequest(String samlAuthnRequest) {
        this.samlRequest = samlAuthnRequest;
        return this;
    }

    public SamlAuthnRequestContainerDtoBuilder withRelayState(String relayState) {
        this.relayState = relayState;
        return this;
    }

    public SamlAuthnRequestContainerDtoBuilder withPrincipalIPAddressAsSeenByHub(String ipAddress) {
        this.principalIPAddressAsSeenByHub = ipAddress;
        return this;
    }
}
