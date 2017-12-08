package uk.gov.ida.hub.policy.domain;

import org.joda.time.DateTime;

import java.net.URI;

public class BaseHubMatchingServiceRequest {

    protected final String requestId;
    protected final String authnRequestIssuerEntityId;
    protected final URI assertionConsumerServiceUri;
    protected final String matchingServiceEntityId;
    protected final DateTime matchingServiceRequestTimeOut;

    protected BaseHubMatchingServiceRequest(String requestId, String authnRequestIssuerEntityId, URI assertionConsumerServiceUri, String matchingServiceEntityId, DateTime matchingServiceRequestTimeOut) {
        this.requestId = requestId;
        this.authnRequestIssuerEntityId = authnRequestIssuerEntityId;
        this.assertionConsumerServiceUri = assertionConsumerServiceUri;
        this.matchingServiceEntityId = matchingServiceEntityId;
        this.matchingServiceRequestTimeOut = matchingServiceRequestTimeOut;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getAuthnRequestIssuerEntityId() {
        return authnRequestIssuerEntityId;
    }

    public URI getAssertionConsumerServiceUri() {
        return assertionConsumerServiceUri;
    }

    public String getMatchingServiceEntityId() {
        return matchingServiceEntityId;
    }

    public DateTime getMatchingServiceRequestTimeOut() { return matchingServiceRequestTimeOut;}
}
