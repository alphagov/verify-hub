package uk.gov.ida.hub.policy.contracts;

import java.net.URI;
import java.util.Optional;

public class AuthnResponseFromHubContainerDto {

    private String samlResponse;
    private URI postEndpoint;
    private Optional<String> relayState = Optional.empty();
    private String responseId;

    @SuppressWarnings("unused") //Needed for JAXB
    private AuthnResponseFromHubContainerDto() {
    }

    public AuthnResponseFromHubContainerDto(
            final String samlResponse,
            final URI postEndpoint,
            final Optional<String> relayState,
            String responseId) {

        this.samlResponse = samlResponse;
        this.postEndpoint = postEndpoint;
        this.relayState = relayState;
        this.responseId = responseId;
    }

    public String getSamlResponse() {
        return samlResponse;
    }

    public URI getPostEndpoint() {
        return postEndpoint;
    }

    public Optional<String> getRelayState() {
        return relayState;
    }

    public String getResponseId() {
        return responseId;
    }
}
