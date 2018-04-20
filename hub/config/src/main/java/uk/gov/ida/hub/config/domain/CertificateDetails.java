package uk.gov.ida.hub.config.domain;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import uk.gov.ida.hub.config.dto.FederationEntityType;

public class CertificateDetails {
    private final String issuerId;
    private final Certificate certificate;
    private final FederationEntityType federationEntityType;
    private final Boolean isEnabled;

    public CertificateDetails(String issuerId, Certificate certificate, FederationEntityType federationEntityType) {
        this.issuerId = issuerId;
        this.certificate = certificate;
        this.federationEntityType = federationEntityType;
        isEnabled = true;
    }

    public CertificateDetails(String issuerId, Certificate certificate, FederationEntityType federationEntityType, Boolean isEnabled) {
        this.issuerId = issuerId;
        this.certificate = certificate;
        this.federationEntityType = federationEntityType;
        this.isEnabled = isEnabled;
    }

    public static CertificateDetails aCertifcateDetail(String issuerId, Certificate certificate, FederationEntityType federationEntityType) {
        return new CertificateDetails(issuerId, certificate, federationEntityType);
    }

    public String getIssuerId() {
        return issuerId;
    }

    public String getX509() {
        return certificate.getX509();
    }

    public CertificateType getKeyUse() {
        return certificate.getCertificateType();
    }

    public FederationEntityType getFederationEntityType() { return federationEntityType; }

    public Boolean isEnabled() {
        return isEnabled;
    }

    public boolean isNotEnabled() {
        return !isEnabled();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        CertificateDetails that = (CertificateDetails) o;

        return new EqualsBuilder()
                .append(issuerId, that.issuerId)
                .append(certificate, that.certificate)
                .append(federationEntityType, that.federationEntityType)
                .append(isEnabled, that.isEnabled)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(issuerId)
                .append(certificate)
                .append(federationEntityType)
                .append(isEnabled)
                .toHashCode();
    }


}
