package uk.gov.ida.integrationtest.hub.policy.apprule.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import httpstub.HttpStubRule;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.domain.SessionId;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

public class SamlSoapProxyProxyStubRule extends HttpStubRule {
    public void setUpStubForSendHubMatchingServiceRequest(SessionId sessionId) throws JsonProcessingException {
        URI uri = UriBuilder
                .fromPath(Urls.SamlSoapProxyUrls.MATCHING_SERVICE_REQUEST_SENDER_RESOURCE)
                .queryParam(Urls.SharedUrls.SESSION_ID_PARAM, sessionId)
                .build();
        Response response = Response.status(Response.Status.ACCEPTED).build();
        register(uri.getPath(), Response.Status.ACCEPTED.getStatusCode(), response);
    }
}
