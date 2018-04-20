package uk.gov.ida.hub.config.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.dropwizard.validation.ValidationMethod;
import org.joda.time.Duration;
import org.joda.time.LocalDateTime;
import uk.gov.ida.hub.config.dto.CertificateExpiryStatus;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;

@JsonIgnoreProperties({ "certificateType", "notAfter", "x509Valid" })
public abstract class Certificate {
    protected String fullCert;

    @ValidationMethod(message = "Certificate was not a valid x509 cert.")
    public boolean isX509Valid() {
        try {
            getCertificate();
        } catch (CertificateException e) {
            return false;
        }
        return true;
    }

    public String getX509() {
        return fullCert
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replace("\n", "")
                .trim();
    }

    private X509Certificate getCertificate() throws CertificateException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(fullCert.getBytes(StandardCharsets.UTF_8));
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(byteArrayInputStream);
    }

    public CertificateExpiryStatus getExpiryStatus(Duration warningPeriod) {
        try {
            Date notAfter = getNotAfter();
            LocalDateTime now = LocalDateTime.now();
            Date notBefore = getNotBefore();
            if (now.toDate().after(notAfter) || now.toDate().before(notBefore)) {
                return CertificateExpiryStatus.CRITICAL;
            }
            if (now.plus(warningPeriod).toDate().after(notAfter)) {
                return CertificateExpiryStatus.WARNING;
            }
            return CertificateExpiryStatus.OK;
        } catch (CertificateException e) {
            return CertificateExpiryStatus.CRITICAL;
        }
    }

    public Date getNotAfter() throws CertificateException {
        return getCertificate().getNotAfter();
    }

    private Date getNotBefore() throws CertificateException {
        return getCertificate().getNotBefore();
    }

    public String getType() {
        return "x509";
    }

    public abstract CertificateType getCertificateType();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Certificate that = (Certificate) o;

        return getX509().equals(that.getX509());
    }
}
