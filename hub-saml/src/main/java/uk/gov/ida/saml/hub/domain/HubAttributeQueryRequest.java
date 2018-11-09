package uk.gov.ida.saml.hub.domain;

import com.google.common.base.Optional;
import javax.validation.constraints.NotNull;
import org.joda.time.DateTime;
import uk.gov.ida.saml.core.domain.AuthnContext;
import uk.gov.ida.saml.core.domain.HubAssertion;
import uk.gov.ida.saml.core.domain.PersistentId;

import java.net.URI;
import java.util.List;

import static java.util.Arrays.asList;

public class HubAttributeQueryRequest extends BaseHubAttributeQueryRequest {
    private List<String> encryptedAssertions;
    private Optional<HubAssertion> cycle3AttributeAssertion;
    private Optional<List<UserAccountCreationAttribute>> userAccountCreationAttributes;
    private AuthnContext authnContext;

    public HubAttributeQueryRequest(
            String id,
            PersistentId persistentId,
            List<String> encryptedAssertions,
            Optional<HubAssertion> cycle3AttributeAssertion,
            Optional<List<UserAccountCreationAttribute>> userAccountCreationAttributes,
            DateTime issueInstant,
            URI assertionConsumerServiceUrl,
            String authnRequestIssuerEntityId,
            AuthnContext authnContext,
            String hubEntityId) {
        super(id, hubEntityId, issueInstant, null, persistentId, assertionConsumerServiceUrl, authnRequestIssuerEntityId);
        this.cycle3AttributeAssertion = cycle3AttributeAssertion;
        this.userAccountCreationAttributes = userAccountCreationAttributes;
        this.authnContext = authnContext;
        this.encryptedAssertions = encryptedAssertions;
    }


    public Optional<HubAssertion> getCycle3AttributeAssertion() {
        return cycle3AttributeAssertion;
    }

    public Optional<List<UserAccountCreationAttribute>> getUserAccountCreationAttributes() {
        return userAccountCreationAttributes;
    }

    public AuthnContext getAuthnContext() {
        return authnContext;
    }

    public List<String> getEncryptedAssertions() {
        return encryptedAssertions;
    }
}