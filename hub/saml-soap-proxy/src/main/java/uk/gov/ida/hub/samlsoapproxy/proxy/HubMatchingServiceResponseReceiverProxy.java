package uk.gov.ida.hub.samlsoapproxy.proxy;

import com.codahale.metrics.annotation.Timed;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.hub.samlsoapproxy.Urls;
import uk.gov.ida.hub.samlsoapproxy.annotations.Policy;
import uk.gov.ida.hub.samlsoapproxy.domain.SamlResponseDto;
import uk.gov.ida.jerseyclient.JsonClient;

import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

public class HubMatchingServiceResponseReceiverProxy {

    private final JsonClient jsonClient;
    private final URI policyUri;

    @Inject
    public HubMatchingServiceResponseReceiverProxy(
            JsonClient jsonClient,
            @Policy URI policyUri) {

        this.jsonClient = jsonClient;
        this.policyUri = policyUri;
    }

    @Timed
    public void notifyHubOfAResponseFromMatchingService(
            SessionId sessionId,
            String base64EncodedSamlResponse) {

        URI uri = UriBuilder
                .fromUri(policyUri)
                .path(Urls.PolicyUrls.ATTRIBUTE_QUERY_RESPONSE_RESOURCE)
                .build(sessionId);

        jsonClient.post(new SamlResponseDto(base64EncodedSamlResponse), uri);

    }

    @Timed
    public void notifyHubOfMatchingServiceRequestFailure(SessionId sessionId) {
        URI uri = UriBuilder
                .fromUri(policyUri)
                .path(Urls.PolicyUrls.MATCHING_SERVICE_REQUEST_FAILURE_RESOURCE)
                .build(sessionId);

        jsonClient.post(null, uri);
    }
}
