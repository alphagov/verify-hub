package uk.gov.ida.integrationtest.hub.samlengine.builders;

import uk.gov.ida.hub.samlengine.contracts.AuthnResponseFromHubContainerDto;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

public class AuthnResponseFromHubContainerDtoBuilder {

    private String responseId = UUID.randomUUID().toString();
    private String samlResponse = UUID.randomUUID().toString();
    private URI postEndPoint = UriBuilder.fromPath(UUID.randomUUID().toString()).build();
    private Optional<String> relayState = Optional.empty();

    public static AuthnResponseFromHubContainerDtoBuilder anAuthnResponseFromHubContainerDto() {
        return new AuthnResponseFromHubContainerDtoBuilder();
    }

    public AuthnResponseFromHubContainerDto build() {
        return new AuthnResponseFromHubContainerDto(samlResponse, postEndPoint, relayState, responseId);
    }

    public AuthnResponseFromHubContainerDtoBuilder withSamlResponse(String samlResponse) {
        this.samlResponse = samlResponse;
        return this;
    }

    public AuthnResponseFromHubContainerDtoBuilder withPostEndPoint(URI uri) {
        this.postEndPoint = uri;
        return this;
    }

    public AuthnResponseFromHubContainerDtoBuilder withResponseId(String responseId) {
        this.responseId = responseId;
        return this;
    }

    public AuthnResponseFromHubContainerDtoBuilder withRelayState(Optional<String> relayState) {
        this.relayState = relayState;
        return this;
    }
}
