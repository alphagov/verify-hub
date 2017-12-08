package uk.gov.ida.hub.policy.domain;

import java.net.URI;

public class AuthnRequestFromHubContainerDto {

    private String samlRequest;
    private URI postEndpoint;
    private boolean registering;

    @SuppressWarnings("unused") //Needed for JAXB
    private AuthnRequestFromHubContainerDto() {
    }

    public AuthnRequestFromHubContainerDto(String samlRequest, URI postEndpoint, boolean registering) {
        this.samlRequest = samlRequest;
        this.postEndpoint = postEndpoint;
        this.registering = registering;
    }

    public String getSamlRequest() {
        return samlRequest;
    }

    public URI getPostEndpoint() {
        return postEndpoint;
    }

    public boolean getRegistering() {
        return registering;
    }
}
