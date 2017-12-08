package uk.gov.ida.hub.policy.proxy;

import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.annotations.SamlSoapProxy;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.jerseyclient.JsonClient;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

@Singleton
public class SamlSoapProxyProxy {

    private final JsonClient jsonClient;
    private final URI samlSoapProxyUri;

    @Inject
    public SamlSoapProxyProxy(@Named("samlSoapProxyClient") JsonClient jsonClient, @SamlSoapProxy URI samlSoapProxyUri) {
        this.jsonClient = jsonClient;
        this.samlSoapProxyUri = samlSoapProxyUri;
    }

    public void sendHubMatchingServiceRequest(SessionId sessionId, AttributeQueryRequest attributeQueryRequest) {
        URI uri = UriBuilder
                .fromUri(samlSoapProxyUri)
                .path(Urls.SamlSoapProxyUrls.MATCHING_SERVICE_REQUEST_SENDER_RESOURCE)
                .queryParam(Urls.SharedUrls.SESSION_ID_PARAM, sessionId)
                .build();
        jsonClient.post(attributeQueryRequest, uri);
    }


}