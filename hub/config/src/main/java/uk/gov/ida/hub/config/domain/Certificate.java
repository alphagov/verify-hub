package uk.gov.ida.hub.config.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.dropwizard.validation.ValidationMethod;
import org.joda.time.Duration;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.config.dto.CertificateExpiryStatus;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;

import static java.text.MessageFormat.format;
import static uk.gov.ida.common.shared.security.Certificate.BEGIN_CERT;
import static uk.gov.ida.common.shared.security.Certificate.END_CERT;

@JsonIgnoreProperties({ "certificateType", "notAfter", "x509Valid" })
public abstract class Certificate {
    private static final Logger LOG = LoggerFactory.getLogger(Certificate.class);
    private static final String FINGERPRINT_ALGORITHM = "SHA-256";

    protected String cert;

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
        return cert;
    }

    private X509Certificate getCertificate() throws CertificateException {
        String fullCert = format("{0}\n{1}\n{2}", BEGIN_CERT, cert.trim(), END_CERT);
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

    public String getSubject() throws CertificateException {
        return getCertificate().getSubjectDN().getName();
    }

    public String getFingerprint() throws CertificateException {
        try {
            final MessageDigest md = MessageDigest.getInstance(FINGERPRINT_ALGORITHM);
            byte[] der = getCertificate().getEncoded();
            md.update(der);
            final byte[] digest = md.digest();
            return DatatypeConverter.printHexBinary(digest);
        } catch (NoSuchAlgorithmException e) {
            LOG.warn(String.format("Algorithm [algorithm = %s] is not available.", FINGERPRINT_ALGORITHM));
        }
        return "";
    }

    public BigInteger getSerialNumber() throws CertificateException {
        return getCertificate().getSerialNumber();
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
