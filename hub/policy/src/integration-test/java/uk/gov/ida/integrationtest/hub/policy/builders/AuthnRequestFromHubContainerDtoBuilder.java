package uk.gov.ida.integrationtest.hub.policy.builders;

import uk.gov.ida.hub.policy.domain.AuthnRequestFromHubContainerDto;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.UUID;

public class AuthnRequestFromHubContainerDtoBuilder {

    private String samlRequest = UUID.randomUUID().toString();
    private URI postEndPoint = UriBuilder.fromPath(UUID.randomUUID().toString()).build();
    private boolean registering = false;

    public static AuthnRequestFromHubContainerDtoBuilder anAuthnRequestFromHubContainerDto() {
        return new AuthnRequestFromHubContainerDtoBuilder();
    }

    public AuthnRequestFromHubContainerDto build(){
        return new AuthnRequestFromHubContainerDto(samlRequest, postEndPoint, registering);
    }

    public AuthnRequestFromHubContainerDtoBuilder withSamlRequest(String samlRequest){
        this.samlRequest = samlRequest;
        return this;
    }

    public AuthnRequestFromHubContainerDtoBuilder withPostEndPoint(URI postEndPoint){
        this.postEndPoint = postEndPoint;
        return this;
    }

    public AuthnRequestFromHubContainerDtoBuilder withRegistering(boolean registering) {
        this.registering = registering;
        return this;
    }
}
