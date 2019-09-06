package uk.gov.ida.hub.policy.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Optional;

// This annotation is required for ZDD where we may add fields to newer versions of this DTO
@JsonIgnoreProperties(ignoreUnknown = true)
public class InboundResponseFromIdpDto {
    private IdpIdaStatus.Status status;
    private String issuer;
    private Optional<String> persistentId;
    private Optional<String> statusMessage;
    private Optional<String> encryptedAuthnAssertion;
    private Optional<String> encryptedMatchingDatasetAssertion;
    private Optional<String> principalIpAddressAsSeenByIdp;
    private Optional<LevelOfAssurance> levelOfAssurance;
    private Optional<String> idpFraudEventId;
    private Optional<String> fraudIndicator;
    private Optional<String> notOnOrAfter;

    public InboundResponseFromIdpDto(IdpIdaStatus.Status status, Optional<String> statusMessage, String issuer, Optional<String> encryptedAuthnAssertion, Optional<String> encryptedMatchingDatasetAssertion, Optional<String> persistentId, Optional<String> principalIpAddressAsSeenByIdp, Optional<LevelOfAssurance> levelOfAssurance, Optional<String> idpFraudEventId, Optional<String> fraudIndicator, Optional<String> notOnOrAfter) {
        this.status = status;
        this.statusMessage = statusMessage;
        this.issuer = issuer;
        this.encryptedAuthnAssertion = encryptedAuthnAssertion;
        this.encryptedMatchingDatasetAssertion = encryptedMatchingDatasetAssertion;
        this.persistentId = persistentId;
        this.principalIpAddressAsSeenByIdp = principalIpAddressAsSeenByIdp;
        this.levelOfAssurance = levelOfAssurance;
        this.idpFraudEventId = idpFraudEventId;
        this.fraudIndicator = fraudIndicator;
        this.notOnOrAfter = notOnOrAfter;
    }

    protected InboundResponseFromIdpDto() {

    }

    public Optional<String> getEncryptedAuthnAssertion() {
        return encryptedAuthnAssertion;
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

    public Optional<String> getNotOnOrAfter(){
        return notOnOrAfter;
    }
}
