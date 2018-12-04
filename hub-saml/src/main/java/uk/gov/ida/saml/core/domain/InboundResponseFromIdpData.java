package uk.gov.ida.saml.core.domain;

import org.joda.time.DateTime;
import uk.gov.ida.saml.hub.domain.IdpIdaStatus;

import java.util.Optional;

public class InboundResponseFromIdpData {
    private IdpIdaStatus.Status status;
    private String issuer;
    private Optional<String> persistentId;
    private Optional<String> statusMessage;
    private Optional<String> encryptedAuthnAssertion;
    private Optional<String> encryptedMatchingDatasetAssertion;
    private Optional<String> principalIpAddressAsSeenByIdp;
    private String levelOfAssurance;
    private Optional<String> idpFraudEventId;
    private Optional<String> fraudIndicator;
    private Optional<DateTime> notOnOrAfter;

    public InboundResponseFromIdpData(
            IdpIdaStatus.Status status,
            Optional<String> statusMessage,
            String issuer,
            Optional<String> encryptedAuthnAssertion,
            Optional<String> encryptedMatchingDatasetAssertion,
            Optional<String> persistentId,
            Optional<String> principalIpAddressAsSeenByIdp,
            String levelOfAssurance,
            Optional<String> idpFraudEventId,
            Optional<String> fraudIndicator,
            Optional<DateTime> notOnOrAfter) {
        this.status = status;
        this.statusMessage = statusMessage;
        this.issuer = issuer;
        this.encryptedAuthnAssertion = encryptedAuthnAssertion;
        this.encryptedMatchingDatasetAssertion = encryptedMatchingDatasetAssertion;
        this.principalIpAddressAsSeenByIdp = principalIpAddressAsSeenByIdp;
        this.persistentId = persistentId;
        this.levelOfAssurance = levelOfAssurance;
        this.idpFraudEventId = idpFraudEventId;
        this.fraudIndicator = fraudIndicator;
        this.notOnOrAfter = notOnOrAfter;

    }

    protected InboundResponseFromIdpData() {}

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

    public Optional<DateTime> getNotOnOrAfter() {
        return notOnOrAfter;
    }

}
