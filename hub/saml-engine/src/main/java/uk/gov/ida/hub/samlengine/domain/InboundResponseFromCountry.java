package uk.gov.ida.hub.samlengine.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Optional;

public class InboundResponseFromCountry {

    private String issuer;
    private Optional<String> persistentId;
    private Optional<String> status;
    private Optional<String> statusMessage;
    private Optional<String> encryptedIdentityAssertionBlob;
    private Optional<LevelOfAssurance> levelOfAssurance;
    @JsonProperty
    private boolean areAssertionsUnsigned;

    private String samlFromCountry;
    private List<String> base64Keys;

    private InboundResponseFromCountry() {
    }

    public InboundResponseFromCountry(
            String issuer,
            Optional<String> persistentId,
            Optional<String> status,
            Optional<String> statusMessage,
            Optional<String> encryptedIdentityAssertionBlob,
            Optional<LevelOfAssurance> levelOfAssurance,
            boolean areAssertionsUnsigned,
            String samlFromCountry,
            List<String> base64Keys
    ) {
        this.issuer = issuer;
        this.persistentId = persistentId;
        this.status = status;
        this.statusMessage = statusMessage;
        this.encryptedIdentityAssertionBlob = encryptedIdentityAssertionBlob;
        this.levelOfAssurance = levelOfAssurance;
        this.areAssertionsUnsigned = areAssertionsUnsigned;
        this.samlFromCountry = samlFromCountry;
        this.base64Keys = base64Keys;
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

    public boolean areAssertionsUnsigned() {
        return areAssertionsUnsigned;
    }

    public String getSamlFromCountry() {
        return samlFromCountry;
    }

    public List<String> getBase64Keys() {
        return this.base64Keys;
    }
}
