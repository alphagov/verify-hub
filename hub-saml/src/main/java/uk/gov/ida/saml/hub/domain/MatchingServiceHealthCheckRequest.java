package uk.gov.ida.saml.hub.domain;

import org.joda.time.DateTime;
import uk.gov.ida.saml.core.domain.PersistentId;
import uk.gov.ida.saml.hub.HubConstants;

import java.net.URI;

public class MatchingServiceHealthCheckRequest extends BaseHubAttributeQueryRequest {

    public MatchingServiceHealthCheckRequest(String id, DateTime issueInstant, PersistentId persistentId, URI assertionConsumerServiceUrl, String authnRequestIssuerEntityId, String hubEntityId) {
        super(id, hubEntityId, issueInstant, null, persistentId, assertionConsumerServiceUrl, authnRequestIssuerEntityId);
    }
}
