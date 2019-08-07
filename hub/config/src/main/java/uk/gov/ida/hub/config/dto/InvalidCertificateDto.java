package uk.gov.ida.hub.config.dto;

import uk.gov.ida.hub.config.domain.CertificateUse;

import java.security.cert.CertPathValidatorException;

public class InvalidCertificateDto {

    private String description;
    private String entityId;
    private String reason;
    private CertificateUse certificateType;
    private FederationEntityType federationType;

    @SuppressWarnings("unused")
    private InvalidCertificateDto() {
    }

    public InvalidCertificateDto(
            final String entityId,
            final CertPathValidatorException.Reason reason,
            final CertificateUse certificateUse,
            final FederationEntityType federationType,
            final String description) {
        this.entityId = entityId;
        this.reason = reason.toString();
        this.certificateType = certificateUse;
        this.federationType = federationType;
        this.description = description;
    }

    public String getReason() {
        return reason;
    }

    public String getEntityId() {
        return entityId;
    }

    public CertificateUse getCertificateType() {
        return certificateType;
    }

    public FederationEntityType getFederationType() {
        return federationType;
    }

    public String getDescription() {
        return description;
    }
}
