package uk.gov.ida.hub.config.dto;

import org.joda.time.Duration;
import org.joda.time.LocalDate;
import uk.gov.ida.hub.config.domain.Certificate;
import uk.gov.ida.hub.config.domain.CertificateType;

import java.security.cert.CertificateException;

public class CertificateHealthCheckDto {
    public static final String EXPIRY_DATE_FORMAT = "EEE dd MMM YYYY";

    private CertificateExpiryStatus status;
    private String message;
    private CertificateType type;
    private String entityId;

    @SuppressWarnings("unused")
    private CertificateHealthCheckDto() {
    }

    private CertificateHealthCheckDto(
            final String entityId,
            final CertificateType type,
            final CertificateExpiryStatus status,
            final String message) {
        this.entityId = entityId;
        this.type = type;
        this.status = status;
        this.message = message;
    }

    public static CertificateHealthCheckDto createCertificateHealthCheckDto(
            String entityId,
            Certificate certificate,
            Duration warningPeriod) throws CertificateException {
        CertificateExpiryStatus status = certificate.getExpiryStatus(warningPeriod);
        String expiryDate = new LocalDate(certificate.getNotAfter()).toString(EXPIRY_DATE_FORMAT);
        String message = getExpiryStatusMessage(status, expiryDate);

        return new CertificateHealthCheckDto(entityId, certificate.getCertificateType(), status, message);
    }

    public String getEntityId() {
        return entityId;
    }

    public CertificateType getType() {
        return type;
    }

    public CertificateExpiryStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    private static String getExpiryStatusMessage(final CertificateExpiryStatus certificateStatus, final String expiryDate) {
        if (certificateStatus == CertificateExpiryStatus.OK) {
            return "";
        }
        if (certificateStatus == CertificateExpiryStatus.WARNING) {
            return "Expires on " + expiryDate;
        }
        return "EXPIRED";
    }
}
