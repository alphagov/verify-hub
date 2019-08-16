package uk.gov.ida.hub.samlengine.domain;

import java.util.Optional;

public class InboundResponseFromCountry {

    private String issuer;
    private Optional<String> persistentId;
    private Optional<String> status;
    private Optional<String> statusMessage;
    private Optional<String> encryptedIdentityAssertionBlob;
    private Optional<LevelOfAssurance> levelOfAssurance;
    private Optional<OriginalSamlWithAssertionDecryptionKeys> originalSamlWithAssertionDecryptionKey;
    private InboundResponseFromCountry() {
    }

    public InboundResponseFromCountry(
            String issuer,
            Optional<String> persistentId,
            Optional<String> status,
            Optional<String> statusMessage,
            Optional<String> encryptedIdentityAssertionBlob,
            Optional<LevelOfAssurance> levelOfAssurance
    ) {
        this.issuer = issuer;
        this.persistentId = persistentId;
        this.status = status;
        this.statusMessage = statusMessage;
        this.encryptedIdentityAssertionBlob = encryptedIdentityAssertionBlob;
        this.levelOfAssurance = levelOfAssurance;
        this.originalSamlWithAssertionDecryptionKey = Optional.empty();
    }

    public String getIssuer() {
        return issuer;
    }

    public Optional<String> getPersistentId() {
        return persistentId;
    }

    public Optional<String> getStatus() {
        return status;
    }

    public Optional<String> getStatusMessage() {
        return statusMessage;
    }

    public Optional<String> getEncryptedIdentityAssertionBlob() {
        return encryptedIdentityAssertionBlob;
    }

    public Optional<LevelOfAssurance> getLevelOfAssurance() {
        return levelOfAssurance;
    }

    public void setOriginalSamlWithAssertionDecryptionKey(OriginalSamlWithAssertionDecryptionKeys originalSamlWithAssertionDecryptionKey) {
        this.originalSamlWithAssertionDecryptionKey = Optional.of(originalSamlWithAssertionDecryptionKey);
    }

    public Optional<OriginalSamlWithAssertionDecryptionKeys> getOriginalSamlWithAssertionDecryptionKey() {
        return originalSamlWithAssertionDecryptionKey;
    }
}
