package uk.gov.ida.hub.samlengine.builders;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import uk.gov.ida.hub.samlengine.domain.Cycle3Dataset;
import uk.gov.ida.hub.samlengine.domain.EidasAttributeQueryRequestDto;
import uk.gov.ida.hub.samlengine.domain.LevelOfAssurance;
import uk.gov.ida.hub.samlengine.domain.PersistentId;
import uk.gov.ida.saml.core.domain.CountrySignedResponseContainer;
import uk.gov.ida.saml.core.test.builders.AssertionBuilder;
import uk.gov.ida.saml.hub.domain.UserAccountCreationAttribute;
import uk.gov.ida.saml.serializers.XmlObjectToBase64EncodedStringTransformer;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.ida.hub.samlengine.builders.PersistentIdBuilder.aPersistentId;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP_MS;

public class EidasAttributeQueryRequestDtoBuilder {

    private String requestId = "request-id";
    private PersistentId persistentId = aPersistentId().buildSamlEnginePersistentId();
    private String encryptedIdentityAssertion = "encrypted-identity-assertion";
    private URI assertionConsumerServiceUri = URI.create("/assertion-consumer-service-uri");
    private String authnRequestIssuerEntityId = TEST_RP;
    private LevelOfAssurance levelOfAssurance = LevelOfAssurance.LEVEL_2;
    private URI matchingServiceAdapterUri = URI.create("/matching-service-adapter-uri");
    private String matchingServiceEntityId = TEST_RP_MS;
    private DateTime matchingServiceRequestTimeOut = DateTime.now(DateTimeZone.UTC).plusMinutes(1);
    private boolean onboarding = true;
    private Optional<Cycle3Dataset> cycle3Dataset = Optional.empty();
    private Optional<List<UserAccountCreationAttribute>> userAccountCreationAttributes = Optional.empty();
    private DateTime assertionExpiry = DateTime.now().plusMinutes(1);
    private Optional<CountrySignedResponseContainer> countrySignedResponse = Optional.empty();

    public static EidasAttributeQueryRequestDtoBuilder anEidasAttributeQueryRequestDto() {
        return new EidasAttributeQueryRequestDtoBuilder();
    }

    public EidasAttributeQueryRequestDto build() {
        return new EidasAttributeQueryRequestDto(
            requestId,
            authnRequestIssuerEntityId,
            assertionConsumerServiceUri,
            assertionExpiry,
            matchingServiceEntityId,
            matchingServiceAdapterUri,
            matchingServiceRequestTimeOut,
            onboarding,
            levelOfAssurance,
            persistentId,
            cycle3Dataset,
            userAccountCreationAttributes,
            encryptedIdentityAssertion,
            countrySignedResponse
        );
    }

    public EidasAttributeQueryRequestDtoBuilder withEncryptedIdentityAssertion(String encryptedAssertion) {
        this.encryptedIdentityAssertion = encryptedAssertion;
        return this;
    }

    public EidasAttributeQueryRequestDtoBuilder withAnEncryptedIdentityAssertion() {
        XmlObjectToBase64EncodedStringTransformer<XMLObject> toBase64EncodedStringTransformer = new XmlObjectToBase64EncodedStringTransformer<>();
        EncryptedAssertion encryptedIdentityAssertion = AssertionBuilder.anAssertion().withId(UUID.randomUUID().toString()).build();
        String encryptedIdentityAssertionString = toBase64EncodedStringTransformer.apply(encryptedIdentityAssertion);
        return withEncryptedIdentityAssertion(encryptedIdentityAssertionString);
    }

    public EidasAttributeQueryRequestDtoBuilder withCycle3Dataset(Cycle3Dataset cycle3Dataset) {
        this.cycle3Dataset = Optional.of(cycle3Dataset);
        return this;
    }

    public EidasAttributeQueryRequestDtoBuilder withCountrySignedResponse(CountrySignedResponseContainer countrySignedResponse) {
        this.countrySignedResponse = Optional.ofNullable(countrySignedResponse);
        return this;
    }

    public EidasAttributeQueryRequestDtoBuilder withACountrySignedResponseWithIssuer(String entityId) {
        CountrySignedResponseContainer countrySignedResponse = new CountrySignedResponseContainer("a saml response", List.of("key"), entityId);
        return withCountrySignedResponse(countrySignedResponse);
    }
}
