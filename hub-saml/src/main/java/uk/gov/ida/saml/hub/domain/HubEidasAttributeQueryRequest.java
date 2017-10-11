package uk.gov.ida.saml.hub.domain;

import org.joda.time.DateTime;
import uk.gov.ida.saml.core.domain.AuthnContext;
import uk.gov.ida.saml.core.domain.PersistentId;

import java.net.URI;

public class HubEidasAttributeQueryRequest extends BaseHubAttributeQueryRequest {

    private final String encryptedIdentityAssertion;
    private final AuthnContext authnContext;

    public HubEidasAttributeQueryRequest(String requestId,
                                         String issuer,
                                         DateTime issueInstant,
                                         PersistentId persistentId, // FIXME - change to use new PersistenceId with equal and hashcode at both places
                                         URI assertionConsumerServiceUrl,
                                         String authnRequestIssuerEntityId,
                                         String encryptedIdentityAssertion,
                                         AuthnContext authnContext) {

        super(requestId, issuer, issueInstant, null, persistentId, assertionConsumerServiceUrl, authnRequestIssuerEntityId);
        this.encryptedIdentityAssertion = encryptedIdentityAssertion;
        this.authnContext = authnContext;
    }

    public String getEncryptedIdentityAssertion() {
        return encryptedIdentityAssertion;
    }

    public AuthnContext getAuthnContext() {
        return authnContext;
    }
}