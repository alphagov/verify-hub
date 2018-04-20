package uk.gov.ida.saml.core.domain;

import java.util.Optional;

import uk.gov.ida.saml.hub.domain.IdpIdaStatus;

public class InboundResponseFromIdpData {
    private IdpIdaStatus.Status status;
    private String issuer;
    private Optional<String> persistentId;
    private Optional<String> statusMessage;
    private Optional<String> authnStatementAssertionBlob;
    private Optional<String> encryptedMatchingDatasetAssertion;
    private Optional<String> principalIpAddressAsSeenByIdp;
    private String levelOfAssurance;
    private Optional<String> idpFraudEventId;
    private Optional<String> fraudIndicator;

    public InboundResponseFromIdpData(
            IdpIdaStatus.Status status,
            Optional<String> statusMessage,
            String issuer,
            Optional<String> authnStatementAssertionBlob,
            Optional<String> encryptedMatchingDatasetAssertion,
            Optional<String> persistentId,
            Optional<String> principalIpAddressAsSeenByIdp,
            String levelOfAssurance,
            Optional<String> idpFraudEventId,
            Optional<String> fraudIndicator) {
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

    protected InboundResponseFromIdpData() {}

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

    public String getLevelOfAssurance() {
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
