package uk.gov.ida.hub.config.dto;

import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import uk.gov.ida.hub.config.domain.Certificate;
import uk.gov.ida.hub.config.domain.CertificateUse;

import java.util.Date;

public class CertificateHealthCheckDto {
    public static final String EXPIRY_DATE_FORMAT = "EEE dd MMM YYYY";

    private CertificateExpiryStatus status;
    private String message;
    private CertificateUse type;
    private String entityId;

    @SuppressWarnings("unused")
    private CertificateHealthCheckDto() {
    }

    public CertificateHealthCheckDto(Certificate certificate, Duration warningPeriod) {
        this.entityId = certificate.getIssuerEntityId();
        this.type = certificate.getCertificateUse();
        this.status = getExpiryStatus(certificate, warningPeriod);
        String expiryDate = new LocalDate(certificate.getNotAfter()).toString(EXPIRY_DATE_FORMAT);
        this.message = getExpiryStatusMessage(this.status, expiryDate);
    }

    public String getEntityId() {
        return entityId;
    }

    public CertificateUse getType() {
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

    private static CertificateExpiryStatus getExpiryStatus(Certificate cert, Duration warningPeriod) {
        Date notAfter = cert.getNotAfter();
        LocalDateTime now = LocalDateTime.now();
        Date notBefore = cert.getNotBefore();
        if (now.toDate().after(notAfter) || now.toDate().before(notBefore)) {
            return CertificateExpiryStatus.CRITICAL;
        }
        if (now.plus(warningPeriod).toDate().after(notAfter)) {
            return CertificateExpiryStatus.WARNING;
        }
        return CertificateExpiryStatus.OK;
    }
}
