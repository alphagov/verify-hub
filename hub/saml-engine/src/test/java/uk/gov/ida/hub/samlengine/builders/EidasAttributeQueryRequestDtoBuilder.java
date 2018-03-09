package uk.gov.ida.hub.samlengine.builders;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import uk.gov.ida.hub.samlengine.domain.Cycle3Dataset;
import uk.gov.ida.hub.samlengine.domain.EidasAttributeQueryRequestDto;
import uk.gov.ida.hub.samlengine.domain.LevelOfAssurance;
import uk.gov.ida.hub.samlengine.domain.PersistentId;
import uk.gov.ida.saml.hub.domain.UserAccountCreationAttribute;

import java.net.URI;
import java.util.List;
import java.util.Optional;

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
            encryptedIdentityAssertion
        );
    }

    public EidasAttributeQueryRequestDtoBuilder withEncryptedIdentityAssertion(String encryptedAssertion) {
        this.encryptedIdentityAssertion = encryptedAssertion;
        return this;
    }

    public EidasAttributeQueryRequestDtoBuilder withCycle3Dataset(Cycle3Dataset cycle3Dataset) {
        this.cycle3Dataset = Optional.of(cycle3Dataset);
        return this;
    }
}
