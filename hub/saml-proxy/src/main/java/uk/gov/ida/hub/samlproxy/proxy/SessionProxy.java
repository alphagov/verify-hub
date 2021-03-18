package uk.gov.ida.hub.samlproxy.proxy;

import com.codahale.metrics.annotation.Timed;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.hub.samlproxy.Urls;
import uk.gov.ida.hub.samlproxy.annotations.Policy;
import uk.gov.ida.hub.samlproxy.contracts.AuthnResponseFromHubContainerDto;
import uk.gov.ida.hub.samlproxy.domain.AuthnRequestFromHubContainerDto;
import uk.gov.ida.hub.samlproxy.domain.ResponseActionDto;
import uk.gov.ida.hub.samlproxy.domain.SamlAuthnRequestContainerDto;
import uk.gov.ida.hub.samlproxy.domain.SamlAuthnResponseContainerDto;
import uk.gov.ida.jerseyclient.JsonClient;

import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

public class SessionProxy {

    private final JsonClient client;
    private final URI policyUri;

    @Inject
    public SessionProxy(JsonClient client, @Policy URI policyUri) {
        this.client = client;
        this.policyUri = policyUri;
    }

    @Timed
    public SessionId createSession(SamlAuthnRequestContainerDto dto) {
        URI uri = UriBuilder
                    .fromUri(policyUri)
                    .path(Urls.PolicyUrls.NEW_SESSION_RESOURCE)
                    .build();
        return client.post(dto, uri, SessionId.class);
    }

    @Timed
    public AuthnRequestFromHubContainerDto getAuthnRequestFromHub(SessionId sessionId) {
        URI uri = UriBuilder
                .fromUri(policyUri)
                .path(Urls.PolicyUrls.IDP_AUTHN_REQUEST_RESOURCE)
                .build(sessionId);

        return client.get(uri, AuthnRequestFromHubContainerDto.class);
    }

    @Timed
    public ResponseActionDto receiveAuthnResponseFromIdp(SamlAuthnResponseContainerDto authnResponseDto, SessionId sessionId) {
        URI uri = UriBuilder
                .fromUri(policyUri)
                .path(Urls.PolicyUrls.IDP_AUTHN_RESPONSE_RESOURCE)
                .build(sessionId);

        return client.post(authnResponseDto, uri, ResponseActionDto.class);
    }

    @Timed
    public AuthnResponseFromHubContainerDto getAuthnResponseFromHub(SessionId sessionId) {
        URI uri = UriBuilder
                .fromUri(policyUri)
                .path(Urls.PolicyUrls.RP_AUTHN_RESPONSE_RESOURCE)
                .build(sessionId);

        return client.get(uri, AuthnResponseFromHubContainerDto.class);
    }

    @Timed
    public AuthnResponseFromHubContainerDto getErrorResponseFromHub(SessionId sessionId) {
        URI uri = UriBuilder
                .fromUri(policyUri)
                .path(Urls.PolicyUrls.RP_ERROR_RESPONSE_RESOURCE)
                .build(sessionId);

        return client.get(uri, AuthnResponseFromHubContainerDto.class);
    }
}
