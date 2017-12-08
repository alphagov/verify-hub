package uk.gov.ida.hub.policy.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Optional;

// This annotation is required for ZDD where we may add fields to newer versions of this DTO
@JsonIgnoreProperties(ignoreUnknown = true)
public class InboundResponseFromIdpDto {
    private IdpIdaStatus.Status status;
    private String issuer;
    private Optional<String> persistentId;
    private Optional<String> statusMessage;
    private Optional<String> authnStatementAssertionBlob;
    private Optional<String> encryptedMatchingDatasetAssertion;
    private Optional<String> principalIpAddressAsSeenByIdp;
    private Optional<LevelOfAssurance> levelOfAssurance;
    private Optional<String> idpFraudEventId;
    private Optional<String> fraudIndicator;

    public InboundResponseFromIdpDto(IdpIdaStatus.Status status, Optional<String> statusMessage, String issuer, Optional<String> authnStatementAssertionBlob, Optional<String> encryptedMatchingDatasetAssertion, Optional<String> persistentId, Optional<String> principalIpAddressAsSeenByIdp, Optional<LevelOfAssurance> levelOfAssurance, Optional<String> idpFraudEventId, Optional<String> fraudIndicator) {
        this.status = status;
        this.statusMessage = statusMessage;
        this.issuer = issuer;
        this.authnStatementAssertionBlob = authnStatementAssertionBlob;
        this.encryptedMatchingDatasetAssertion = encryptedMatchingDatasetAssertion;
        this.principalIpAddressAsSeenByIdp = principalIpAddressAsSeenByIdp;
        this.persistentId = persistentId;
        this.levelOfAssurance = levelOfAssurance;
        this.idpFraudEventId = idpFraudEventId;
        this.fraudIndicator = fraudIndicator;
    }

    protected InboundResponseFromIdpDto() {

    }

    public Optional<String> getAuthnStatementAssertionBlob() {
        return authnStatementAssertionBlob;
    }

    public IdpIdaStatus.Status getStatus() {
        return status;
    }

    public String getIssuer() {
        return issuer;
    }

    public Optional<String> getStatusMessage() {
        return statusMessage;
    }

    public Optional<String> getPrincipalIpAddressAsSeenByIdp() {
        return principalIpAddressAsSeenByIdp;
    }

    public Optional<String> getPersistentId() {
        return persistentId;
    }

    public Optional<LevelOfAssurance> getLevelOfAssurance() {
        return levelOfAssurance;
    }

    public Optional<String> getIdpFraudEventId() {
        return idpFraudEventId;
    }

    public Optional<String> getFraudIndicator() {
        return fraudIndicator;
    }

    public Optional<String> getEncryptedMatchingDatasetAssertion() {
        return encryptedMatchingDatasetAssertion;
    }
}
