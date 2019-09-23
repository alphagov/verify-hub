package uk.gov.ida.saml.core.domain;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public class AuthnResponseFromCountryContainerDto {

    private String samlResponse;
    private List<String> encryptedKeys;
    private URI postEndpoint;
    private Optional<String> relayState = Optional.empty();
    private String inResponseTo;
    private String responseId;

    @SuppressWarnings("unused") //Needed for JAXB
    private AuthnResponseFromCountryContainerDto() {
    }

    public AuthnResponseFromCountryContainerDto(
            final EidasCountrySignedResponseWithEncryptedKeys signedResponseWithEncryptedKeys,
            final URI postEndpoint,
            final Optional<String> relayState,
            String responseId,
            String inResponseTo) {

        this.samlResponse = signedResponseWithEncryptedKeys.getSaml();
        this.encryptedKeys = signedResponseWithEncryptedKeys.getBase64encryptedKeys();
        this.postEndpoint = postEndpoint;
        this.relayState = relayState;
        this.inResponseTo = inResponseTo;
        this.responseId = responseId;
    }

    public String getSamlResponse() { return samlResponse; }

    public List<String> getEncryptedKeys() { return encryptedKeys; }

    public URI getPostEndpoint() {
        return postEndpoint;
    }

    public Optional<String> getRelayState() {
        return relayState;
    }

    public String getInResponseTo() { return inResponseTo; }

    public String getResponseId() {
        return responseId;
    }
}
