package uk.gov.ida.hub.config.dto;

import uk.gov.ida.hub.config.domain.CertificateType;

import java.security.cert.CertPathValidatorException;

public class InvalidCertificateDto {

    private String description;
    private String entityId;
    private String reason;
    private CertificateType certificateType;
    private FederationEntityType federationType;

    @SuppressWarnings("unused")
    private InvalidCertificateDto() {
    }

    public InvalidCertificateDto(
            final String entityId,
            final CertPathValidatorException.Reason reason,
            final CertificateType certificateType,
            final FederationEntityType federationType,
            final String description) {
        this.entityId = entityId;
        this.reason = reason.toString();
        this.certificateType = certificateType;
        this.federationType = federationType;
        this.description = description;
    }

    public String getReason() {
        return reason;
    }

    public String getEntityId() {
        return entityId;
    }

    public CertificateType getCertificateType() {
        return certificateType;
    }

    public FederationEntityType getFederationType() {
        return federationType;
    }

    public String getDescription() {
        return description;
    }
}
