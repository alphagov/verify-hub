package uk.gov.ida.hub.samlengine.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.saml.hub.domain.UserAccountCreationAttribute;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;

// This annotation is required for ZDD where we may add fields to newer versions of this DTO
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttributeQueryRequestDto {
    protected String requestId;
    protected String authnRequestIssuerEntityId;
    protected URI assertionConsumerServiceUri;
    protected String matchingServiceEntityId;
    private DateTime matchingServiceRequestTimeOut;
    private LevelOfAssurance levelOfAssurance;
    @NotNull
    private String encryptedMatchingDatasetAssertion;

    private String authnStatementAssertion;
    private Optional<Cycle3Dataset> cycle3Dataset;
    private Optional<List<UserAccountCreationAttribute>> userAccountCreationAttributes;
    private PersistentId persistentId;
    private DateTime assertionExpiry;
    private URI attributeQueryUri;
    private boolean onboarding;

    @SuppressWarnings("unused") // needed by jaxb
    private AttributeQueryRequestDto() {
    }

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
        final String encryptedMatchingDatasetAssertion,
        final String authnStatementAssertion) {

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
        this.encryptedMatchingDatasetAssertion = encryptedMatchingDatasetAssertion;
        this.authnStatementAssertion = authnStatementAssertion;
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

    public DateTime getMatchingServiceRequestTimeOut() { return matchingServiceRequestTimeOut;}

    public String getAuthnStatementAssertion() {
        return authnStatementAssertion;
    }

    public Optional<Cycle3Dataset> getCycle3Dataset() {
        return cycle3Dataset;
    }

    public Optional<List<UserAccountCreationAttribute>> getUserAccountCreationAttributes() {
        return userAccountCreationAttributes;
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

    public URI getAttributeQueryUri() {
        return attributeQueryUri;
    }

    public String getRequestId() {
        return requestId;
    }


    public boolean isOnboarding() {
        return onboarding;
    }

    public String getEncryptedMatchingDatasetAssertion() {
        return encryptedMatchingDatasetAssertion;
    }
}
