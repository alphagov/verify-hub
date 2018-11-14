package uk.gov.ida.hub.policy.contracts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.Cycle3Dataset;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.UserAccountCreationAttribute;
import uk.gov.ida.hub.policy.proxy.SamlEngineProxy;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

// This annotation is required for ZDD where we may add fields to newer versions of this DTO
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AttributeQueryRequestDto extends AbstractAttributeQueryRequestDto {

    private final List<String> encryptedAssertions;

    public AttributeQueryRequestDto(
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
        final List<String> encryptedAssertions) {

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
        this.encryptedAssertions = encryptedAssertions;
    }

    public static AttributeQueryRequestDto createCycle3MatchingServiceRequest(
            String requestId,
            String encryptedMatchingDatasetAssertion,
            String encryptedAuthnAssertion,
            Cycle3Dataset cycle3Assertion,
            String authnRequestIssuerEntityId,
            URI assertionConsumerServiceUri,
            String matchingServiceEntityId,
            DateTime matchingServiceRequestTimeOut,
            LevelOfAssurance levelOfAssurance,
            Optional<List<UserAccountCreationAttribute>> userAccountCreationAttributes,
            PersistentId persistentId,
            DateTime assertionExpiry,
            URI attributeQueryUri,
            boolean onboarding) {

        return new AttributeQueryRequestDto(
            requestId,
            authnRequestIssuerEntityId,
            assertionConsumerServiceUri,
            assertionExpiry, matchingServiceEntityId,
            attributeQueryUri,
            matchingServiceRequestTimeOut,
            onboarding,
            levelOfAssurance,
            persistentId,
            Optional.fromNullable(cycle3Assertion),
            userAccountCreationAttributes,
            asList(encryptedMatchingDatasetAssertion, encryptedAuthnAssertion)
        );
    }

    public static AttributeQueryRequestDto createCycle01MatchingServiceRequest(
            String requestId,
            String encryptedMatchingDatasetAssertion,
            String encryptedAuthnAssertion,
            String authnRequestIssuerEntityId,
            URI assertionConsumerServiceUri,
            String matchingServiceEntityId,
            DateTime matchingServiceRequestTimeOut,
            LevelOfAssurance levelOfAssurance,
            PersistentId persistentId,
            DateTime assertionExpiry,
            URI attributeQueryUri,
            boolean onboarding) {

        return new AttributeQueryRequestDto(
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
            Optional.absent(),
            Optional.absent(),
            asList(encryptedMatchingDatasetAssertion, encryptedAuthnAssertion)
        );
    }

    public static AttributeQueryRequestDto createUserAccountRequiredMatchingServiceRequest(
            String requestId,
            String encryptedMatchingDatasetAssertion,
            String encryptedAuthnAssertion,
            Optional<Cycle3Dataset> cycle3Assertion,
            String authnRequestIssuerEntityId,
            URI assertionConsumerServiceUri,
            String matchingServiceEntityId,
            DateTime matchingServiceRequestTimeOut,
            LevelOfAssurance levelOfAssurance,
            List<UserAccountCreationAttribute> userAccountCreationAttributes,
            PersistentId persistentId,
            DateTime assertionExpiry,
            URI attributeQueryUri,
            boolean onboarding) {

        return new AttributeQueryRequestDto(
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
            cycle3Assertion,
            Optional.fromNullable(userAccountCreationAttributes),
            asList(encryptedMatchingDatasetAssertion, encryptedAuthnAssertion)
        );
    }

    public List<String> getEncryptedAssertions() {
        return encryptedAssertions.stream().filter(Objects::nonNull).collect(toList());
    }

    @Override
    public AttributeQueryContainerDto sendToSamlEngine(SamlEngineProxy samlEngineProxy) {
        return samlEngineProxy.generateAttributeQuery(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof AttributeQueryRequestDto)) {
            return false;
        }

        AttributeQueryRequestDto that = (AttributeQueryRequestDto) o;

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
            Objects.equals(getEncryptedAssertions(), that.getEncryptedAssertions());
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
            getEncryptedAssertions());
    }
}
