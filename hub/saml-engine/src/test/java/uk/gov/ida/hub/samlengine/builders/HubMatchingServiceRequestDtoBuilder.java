package uk.gov.ida.hub.samlengine.builders;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.samlengine.domain.AttributeQueryRequestDto;
import uk.gov.ida.hub.samlengine.domain.Cycle3Dataset;
import uk.gov.ida.hub.samlengine.domain.LevelOfAssurance;
import uk.gov.ida.hub.samlengine.domain.PersistentId;
import uk.gov.ida.saml.hub.domain.UserAccountCreationAttribute;

import java.net.URI;
import java.util.List;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static uk.gov.ida.hub.samlengine.builders.PersistentIdBuilder.aPersistentId;

public class HubMatchingServiceRequestDtoBuilder {
    private String id = "id";
    private String authnStatementAssertion = "aPassthroughAssertion().buildAuthnStatementAssertion()";
    private Optional<Cycle3Dataset> cycle3AttributeAssertion = absent();
    private Optional<List<UserAccountCreationAttribute>> userAccountCreationAttributes = absent();
    private String authnRequestIssuerEntityId = "default-auth-request-issuer-id-from-builder";
    private URI assertionConsumerServiceUri = URI.create("/default-ac-service-uri");
    private String matchingServiceEntityId = "matching-service-entity-id";
    private DateTime matchingServiceRequestTimeOut = DateTime.now().plusSeconds(90); //90 is deliberately wrong, but feasible (really it's a minute) //Andrew 27/Jan/2014
    private PersistentId persistentId = aPersistentId().buildSamlEnginePersistentId();
    private URI attributeQueryUri = URI.create("/default-msa-uri");
    private boolean onboarding;
    private String encryptedMatchingDatasetAssertion = "encrypted-matching-service-assertion";

    public static HubMatchingServiceRequestDtoBuilder aHubMatchingServiceRequestDto() {
        return new HubMatchingServiceRequestDtoBuilder();
    }

    public AttributeQueryRequestDto build() {
        return new AttributeQueryRequestDto(
            id,
            authnRequestIssuerEntityId,
            assertionConsumerServiceUri,
            DateTime.now(),
            matchingServiceEntityId,
            attributeQueryUri,
            matchingServiceRequestTimeOut,
            onboarding,
            LevelOfAssurance.LEVEL_1,
            persistentId,
            cycle3AttributeAssertion,
            userAccountCreationAttributes,
            encryptedMatchingDatasetAssertion,
            authnStatementAssertion);
    }

    public HubMatchingServiceRequestDtoBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public HubMatchingServiceRequestDtoBuilder withEncryptedMatchingDatasetAssertion(String encryptedAssertion) {
        this.encryptedMatchingDatasetAssertion = encryptedAssertion;
        return this;
    }

    public HubMatchingServiceRequestDtoBuilder withAuthnStatementAssertion(String authnStatementAssertion) {
        this.authnStatementAssertion = authnStatementAssertion;
        return this;
    }

    public HubMatchingServiceRequestDtoBuilder withCycle3DataAssertion(Cycle3Dataset cycle3Assertion) {
        this.cycle3AttributeAssertion = fromNullable(cycle3Assertion);
        return this;
    }

    public HubMatchingServiceRequestDtoBuilder withMatchingServiceEntityId(String matchingServiceEntityId) {
        this.matchingServiceEntityId = matchingServiceEntityId;
        return this;
    }

    public HubMatchingServiceRequestDtoBuilder withAuthnRequestIssuerEntityId(String entityId) {
        this.authnRequestIssuerEntityId = entityId;
        return this;
    }

    public HubMatchingServiceRequestDtoBuilder withPersistentId(PersistentId persistentId) {
        this.persistentId = persistentId;
        return this;
    }

    public HubMatchingServiceRequestDtoBuilder withUserAccountCreationAttributes(
            final List<UserAccountCreationAttribute> attributes) {

        this.userAccountCreationAttributes = fromNullable(attributes);
        return this;
    }

    public HubMatchingServiceRequestDtoBuilder withAttributeQueryUri(URI attributeQueryUri) {
        this.attributeQueryUri = attributeQueryUri;
        return this;
    }


}
