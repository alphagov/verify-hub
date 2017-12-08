package uk.gov.ida.hub.policy.builder;

import com.google.common.base.Optional;
import uk.gov.ida.hub.policy.contracts.AuthnResponseFromHubContainerDto;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.UUID;

public class AuthnResponseFromHubContainerDtoBuilder {

    private String responseId = UUID.randomUUID().toString();
    private String samlResponse = UUID.randomUUID().toString();
    private URI postEndPoint = UriBuilder.fromPath(UUID.randomUUID().toString()).build();
    private Optional<String> relayState = Optional.absent();

    public static AuthnResponseFromHubContainerDtoBuilder anAuthnResponseFromHubContainerDto() {
        return new AuthnResponseFromHubContainerDtoBuilder();
    }

    public AuthnResponseFromHubContainerDto build() {
        return new AuthnResponseFromHubContainerDto(samlResponse, postEndPoint, relayState, responseId);
    }
}

