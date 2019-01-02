package uk.gov.ida.saml.hub.test.builders;

import java.util.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.saml.core.domain.AuthnContext;
import uk.gov.ida.saml.core.domain.HubAssertion;
import uk.gov.ida.saml.core.domain.PersistentId;
import uk.gov.ida.saml.hub.domain.HubAttributeQueryRequest;
import uk.gov.ida.saml.hub.domain.UserAccountCreationAttribute;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

public class HubAttributeQueryRequestBuilder {

    private String id = "id";
    private PersistentId persistentId = new PersistentId("default-name-id");
    private String encryptedAuthnAssertion = "aPassthroughAssertion().buildAuthnStatementAssertion()";
    private Optional<HubAssertion> cycle3AttributeAssertion = empty();
    private Optional<List<UserAccountCreationAttribute>> userAccountCreationAttributes = empty();
    private URI assertionConsumerServiceUrl = URI.create("http://transaction.com");
    private String authnRequestIssuerEntityId = "issuer-id";
    private AuthnContext authnContext = AuthnContext.LEVEL_1;
    private String encryptedMathcingDatasetAssertion = "aPassthroughAssertion().buildEncryptedMatchingDatasetAssertion()";
    private String hubEntityId = "hubEntityId";

    public static HubAttributeQueryRequestBuilder aHubAttributeQueryRequest() {
        return new HubAttributeQueryRequestBuilder();
    }

    public HubAttributeQueryRequest build() {
        return new HubAttributeQueryRequest(
                id,
                persistentId,
                encryptedMathcingDatasetAssertion,
                encryptedAuthnAssertion,
                cycle3AttributeAssertion,
                userAccountCreationAttributes,
                DateTime.now(),
                assertionConsumerServiceUrl,
                authnRequestIssuerEntityId,
                authnContext,
                hubEntityId
        );
    }

    public HubAttributeQueryRequestBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public HubAttributeQueryRequestBuilder withPersistentId(PersistentId persistentId) {
        this.persistentId = persistentId;
        return this;
    }

    public HubAttributeQueryRequestBuilder withEncryptedMatchingDatasetAssertion(String assertion) {
        this.encryptedMathcingDatasetAssertion = assertion;
        return this;
    }

    public HubAttributeQueryRequestBuilder withEncryptedAuthnAssertion(String authnStatementAssertion) {
        this.encryptedAuthnAssertion = authnStatementAssertion;
        return this;
    }

    public HubAttributeQueryRequestBuilder withCycle3DataAssertion(HubAssertion cycle3DataAssertion) {
        this.cycle3AttributeAssertion = ofNullable(cycle3DataAssertion);
        return this;
    }

    public HubAttributeQueryRequestBuilder withAssertionConsumerServiceUrl(URI assertionConsumerServiceUrl) {
        this.assertionConsumerServiceUrl = assertionConsumerServiceUrl;
        return this;
    }

    public HubAttributeQueryRequestBuilder withAuthnRequestIssuerEntityId(String requestIssuer) {
        this.authnRequestIssuerEntityId = requestIssuer;
        return this;
    }

    public HubAttributeQueryRequestBuilder withAuthnContext(AuthnContext authnContext) {
        this.authnContext = authnContext;
        return this;
    }

    public HubAttributeQueryRequestBuilder addUserAccountCreationAttribute(final UserAccountCreationAttribute userAccountCreationAttribute) {
        if(!userAccountCreationAttributes.isPresent()){
           List<UserAccountCreationAttribute> userAccountCreationAttributeList = new ArrayList<>();

           userAccountCreationAttributes = Optional.ofNullable(userAccountCreationAttributeList);
        }
        this.userAccountCreationAttributes.get().add(userAccountCreationAttribute);
        return this;
    }

    public HubAttributeQueryRequestBuilder withoutUserAccountCreationAttributes() {
        userAccountCreationAttributes = empty();
        return this;
    }
}
