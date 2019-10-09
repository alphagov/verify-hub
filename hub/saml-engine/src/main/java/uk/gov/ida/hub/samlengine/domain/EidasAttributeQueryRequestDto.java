package uk.gov.ida.hub.samlengine.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.joda.time.DateTime;
import uk.gov.ida.saml.core.domain.CountrySignedResponseContainer;
import uk.gov.ida.saml.hub.domain.UserAccountCreationAttribute;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.Optional;

// This annotation is required for ZDD where we may add fields to newer versions of this DTO
@JsonIgnoreProperties(ignoreUnknown = true)
public class EidasAttributeQueryRequestDto {

    private String requestId;
    private String authnRequestIssuerEntityId;
    private URI assertionConsumerServiceUri;
    private DateTime assertionExpiry;
    private String matchingServiceEntityId;
    private URI attributeQueryUri;
    private DateTime matchingServiceRequestTimeOut;
    private boolean onboarding;
    private LevelOfAssurance levelOfAssurance;
    private PersistentId persistentId;
    private Optional<Cycle3Dataset> cycle3Dataset;
    private Optional<List<UserAccountCreationAttribute>> userAccountCreationAttributes;
    @NotNull
    private String encryptedIdentityAssertion;
    @NotNull
    private Optional<CountrySignedResponseContainer> countrySignedResponseContainer;

    @SuppressWarnings("unused") // needed by jaxb
    private EidasAttributeQueryRequestDto() {}

    public EidasAttributeQueryRequestDto(
            final String requestId,
            final String authnRequestIssuerEntityId,
            final URI assertionConsumerServiceUri,
            final DateTime assertionExpiry,
            final String matchingServiceEntityId,
            final URI attributeQueryUri,
            final DateTime matchingServiceRequestTimeOut,
            final boolean onboarding,
            final LevelOfAssurance levelOfAssurance,
            final PersistentId persistentId,
            final Optional<Cycle3Dataset> cycle3Dataset,
            final Optional<List<UserAccountCreationAttribute>> userAccountCreationAttributes,
            final String encryptedIdentityAssertion,
            final Optional<CountrySignedResponseContainer> countrySignedResponseContainer) {

        this.requestId = requestId;
        this.authnRequestIssuerEntityId = authnRequestIssuerEntityId;
        this.assertionConsumerServiceUri = assertionConsumerServiceUri;
        this.assertionExpiry = assertionExpiry;
        this.matchingServiceEntityId = matchingServiceEntityId;
        this.attributeQueryUri = attributeQueryUri;
        this.matchingServiceRequestTimeOut = matchingServiceRequestTimeOut;
        this.onboarding = onboarding;
        this.levelOfAssurance = levelOfAssurance;
        this.persistentId = persistentId;
        this.cycle3Dataset = cycle3Dataset;
        this.userAccountCreationAttributes = userAccountCreationAttributes;
        this.encryptedIdentityAssertion = encryptedIdentityAssertion;
        this.countrySignedResponseContainer = countrySignedResponseContainer;
    }

    public String getRequestId() {
        return requestId;
    }

    public PersistentId getPersistentId() {
        return persistentId;
    }

    public String getEncryptedIdentityAssertion() {
        return encryptedIdentityAssertion;
    }

    public URI getAssertionConsumerServiceUri() {
        return assertionConsumerServiceUri;
    }

    public String getAuthnRequestIssuerEntityId() {
        return authnRequestIssuerEntityId;
    }

    public LevelOfAssurance getLevelOfAssurance() {
        return levelOfAssurance;
    }

    public URI getAttributeQueryUri() {
        return attributeQueryUri;
    }

    public String getMatchingServiceEntityId() {
        return matchingServiceEntityId;
    }

    public DateTime getMatchingServiceRequestTimeOut() {
        return matchingServiceRequestTimeOut;
    }

    public boolean isOnboarding() {
        return onboarding;
    }

    public Optional<Cycle3Dataset> getCycle3Dataset() {
        return cycle3Dataset;
    }

    public Optional<List<UserAccountCreationAttribute>> getUserAccountCreationAttributes() {
        return userAccountCreationAttributes;
    }

    public DateTime getAssertionExpiry() {
        return assertionExpiry;
    }

    public Optional<CountrySignedResponseContainer> getCountrySignedResponseContainer() {
        return countrySignedResponseContainer;
    }
}
