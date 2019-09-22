package uk.gov.ida.hub.policy.contracts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.Cycle3Dataset;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.UserAccountCreationAttribute;
import uk.gov.ida.hub.policy.proxy.SamlEngineProxy;
import uk.gov.ida.saml.core.domain.CountrySignedResponseContainer;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

// This annotation is required for ZDD where we may add fields to newer versions of this DTO
@JsonIgnoreProperties(ignoreUnknown = true)
public final class EidasAttributeQueryRequestDto extends AbstractAttributeQueryRequestDto {

    @NotNull
    private final String encryptedIdentityAssertion;
    @NotNull
    private final Optional<CountrySignedResponseContainer> countrySignedResponse;

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
            final Optional<CountrySignedResponseContainer> countrySignedResponse) {

        super(
            requestId,
            authnRequestIssuerEntityId,
            assertionConsumerServiceUri,
            assertionExpiry,
            matchingServiceEntityId,
            attributeQueryUri,
            matchingServiceRequestTimeOut,
            onboarding,
            levelOfAssurance,
            persistentId,
            cycle3Dataset,
            userAccountCreationAttributes);
        this.encryptedIdentityAssertion = encryptedIdentityAssertion;
        this.countrySignedResponse = countrySignedResponse;
    }

    public String getEncryptedIdentityAssertion() {
        return encryptedIdentityAssertion;
    }

    public Optional<CountrySignedResponseContainer> getCountrySignedResponse() {
        return countrySignedResponse;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("EidasAttributeQueryRequestDto{");
        sb.append(super.toString());
        sb.append(",encryptedIdentityAssertion='").append(encryptedIdentityAssertion).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof EidasAttributeQueryRequestDto)) {
            return false;
        }

        EidasAttributeQueryRequestDto that = (EidasAttributeQueryRequestDto) o;

        return Objects.equals(getRequestId(), that.getRequestId()) &&
            Objects.equals(getAuthnRequestIssuerEntityId(), that.getAuthnRequestIssuerEntityId()) &&
            Objects.equals(getAssertionConsumerServiceUri(), that.getAssertionConsumerServiceUri()) &&
            Objects.equals(getAssertionExpiry(), that.getAssertionExpiry()) &&
            Objects.equals(getMatchingServiceEntityId(), that.getMatchingServiceEntityId()) &&
            Objects.equals(getAttributeQueryUri(), that.getAttributeQueryUri()) &&
            Objects.equals(getMatchingServiceRequestTimeOut(), that.getMatchingServiceRequestTimeOut()) &&
            isOnboarding() == that.isOnboarding() &&
            getLevelOfAssurance() == that.getLevelOfAssurance() &&
            Objects.equals(getPersistentId(), that.getPersistentId()) &&
            Objects.equals(getCycle3Dataset(), that.getCycle3Dataset()) &&
            Objects.equals(getUserAccountCreationAttributes(), that.getUserAccountCreationAttributes()) &&
            Objects.equals(encryptedIdentityAssertion, that.encryptedIdentityAssertion) &&
            Objects.equals(countrySignedResponse, that.countrySignedResponse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            getRequestId(),
            getAuthnRequestIssuerEntityId(),
            getAssertionConsumerServiceUri(),
            getAssertionExpiry(),
            getMatchingServiceEntityId(),
            getAttributeQueryUri(),
            getMatchingServiceRequestTimeOut(),
            isOnboarding(),
            getLevelOfAssurance(),
            getPersistentId(),
            getCycle3Dataset(),
            getUserAccountCreationAttributes(),
            encryptedIdentityAssertion,
            countrySignedResponse);
    }

    @Override
    public AttributeQueryContainerDto sendToSamlEngine(SamlEngineProxy samlEngineProxy) {
        return samlEngineProxy.generateEidasAttributeQuery(this);
    }
}
