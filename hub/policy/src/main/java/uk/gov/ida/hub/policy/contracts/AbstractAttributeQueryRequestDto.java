package uk.gov.ida.hub.policy.contracts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.Cycle3Dataset;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.UserAccountCreationAttribute;
import uk.gov.ida.hub.policy.proxy.SamlEngineProxy;

import java.net.URI;
import java.util.List;
import java.util.Optional;

// This annotation is required for ZDD where we may add fields to newer versions of this DTO
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractAttributeQueryRequestDto {

    private final String requestId;
    private final String authnRequestIssuerEntityId;
    private final URI assertionConsumerServiceUri;
    private final DateTime assertionExpiry;
    private final String matchingServiceEntityId;
    private final URI attributeQueryUri;
    private final DateTime matchingServiceRequestTimeOut;
    private final boolean onboarding;
    private final LevelOfAssurance levelOfAssurance;
    private final PersistentId persistentId;
    private final Optional<Cycle3Dataset> cycle3Dataset;
    private final Optional<List<UserAccountCreationAttribute>> userAccountCreationAttributes;

    public AbstractAttributeQueryRequestDto(
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
        final Optional<List<UserAccountCreationAttribute>> userAccountCreationAttributes) {

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
    }

    public URI getAttributeQueryUri() {
        return attributeQueryUri;
    }

    public Optional<Cycle3Dataset> getCycle3Dataset() {
        return cycle3Dataset;
    }

    public DateTime getMatchingServiceRequestTimeOut() {
        return matchingServiceRequestTimeOut;
    }

    public LevelOfAssurance getLevelOfAssurance() {
        return levelOfAssurance;
    }

    public PersistentId getPersistentId() {
        return persistentId;
    }

    public DateTime getAssertionExpiry() {
        return assertionExpiry;
    }

    public Optional<List<UserAccountCreationAttribute>> getUserAccountCreationAttributes() {
        return userAccountCreationAttributes;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getAuthnRequestIssuerEntityId() {
        return authnRequestIssuerEntityId;
    }

    public URI getAssertionConsumerServiceUri() {
        return assertionConsumerServiceUri;
    }

    public String getMatchingServiceEntityId() {
        return matchingServiceEntityId;
    }

    public boolean isOnboarding() {return onboarding;}

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("requestId='").append(requestId).append('\'');
        sb.append(",authnRequestIssuerEntityId='").append(authnRequestIssuerEntityId).append('\'');
        sb.append(",assertionConsumerServiceUri=").append(assertionConsumerServiceUri);
        sb.append(",assertionExpiry=").append(assertionExpiry);
        sb.append(",matchingServiceEntityId='").append(matchingServiceEntityId).append('\'');
        sb.append(",attributeQueryUri=").append(attributeQueryUri);
        sb.append(",matchingServiceRequestTimeOut=").append(matchingServiceRequestTimeOut);
        sb.append(",onboarding=").append(onboarding);
        sb.append(",levelOfAssurance=").append(levelOfAssurance);
        sb.append(",persistentId=").append(persistentId);
        sb.append(",cycle3Dataset=").append(cycle3Dataset);
        sb.append(",userAccountCreationAttributes=").append(userAccountCreationAttributes);
        return sb.toString();
    }

    public abstract AttributeQueryContainerDto sendToSamlEngine(SamlEngineProxy samlEngineProxy);
}
