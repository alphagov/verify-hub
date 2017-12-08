package uk.gov.ida.hub.config.domain;

import uk.gov.ida.hub.config.dto.FederationEntityType;

public class CertificateDetails {
    private final String issuerId;
    private final Certificate certificate;
    private final FederationEntityType federationEntityType;

    public CertificateDetails(String issuerId, Certificate certificate, FederationEntityType federationEntityType) {
        this.issuerId = issuerId;
        this.certificate = certificate;
        this.federationEntityType = federationEntityType;
    }

    public String getIssuerId() {
        return issuerId;
    }

    public String getX509() {
        return certificate.getX509();
    }

    public CertificateType getKeyUse() {
        return certificate.getType();
    }

    public FederationEntityType getFederationEntityType() { return federationEntityType; }
}
