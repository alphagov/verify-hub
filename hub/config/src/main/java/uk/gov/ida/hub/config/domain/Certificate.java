package uk.gov.ida.hub.config.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.config.dto.FederationEntityType;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;

public class Certificate {
    private static final Logger LOG = LoggerFactory.getLogger(Certificate.class);
    private static final String FINGERPRINT_ALGORITHM = "SHA-256";

    private final String issuerEntityId;
    private final FederationEntityType federationEntityType;
    private final String base46EncodeCertificate;
    private final CertificateType certificateType;
    private final boolean enabled;
    private final X509Certificate x509Certificate;
    private final boolean valid;

    public Certificate(String issuerEntityId, FederationEntityType federationEntityType, String base46EncodeCertificate, CertificateType certificateType, boolean enabled){
        this.issuerEntityId = issuerEntityId;
        this.federationEntityType = federationEntityType;
        this.base46EncodeCertificate = base46EncodeCertificate;
        this.certificateType = certificateType;
        this.enabled = enabled;
        X509Certificate x509 = null;
        try {
            x509 = getCertificate(base46EncodeCertificate);
        } catch (CertificateException e) {

        }
        this.x509Certificate = x509;
        this.valid = x509 != null;
    }

    public String getIssuerEntityId() {
        return issuerEntityId;
    }

    public FederationEntityType getFederationEntityType() {
        return federationEntityType;
    }

    public boolean isValid() {
        return valid;
    }

    public String getX509() {
        if (x509Certificate != null){
            try {
                return Base64.getEncoder().encodeToString(x509Certificate.getEncoded());
            } catch (CertificateEncodingException e) {
                return "";
            }
        }
        return "";
    }

    private X509Certificate getCertificate(String base64EncodeCertificate) throws CertificateException {
        String cleanUpRegex = "(-----BEGIN CERTIFICATE-----)|(\\n)|(-----END CERTIFICATE-----)";
        String clean64 = base64EncodeCertificate.replaceAll(cleanUpRegex, "");
        try {
            byte[] certBytes = Base64.getDecoder().decode(clean64);
            return (X509Certificate) CertificateFactory
                    .getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(certBytes));
        } catch (IllegalArgumentException e) {
            throw new CertificateException(e);
        }
    }

    public Date getNotAfter() {
        return x509Certificate.getNotAfter();
    }

    public String getSubject() {
        return x509Certificate.getSubjectDN().getName();
    }

    public String getFingerprint() throws CertificateException{
        try {
            final MessageDigest md = MessageDigest.getInstance(FINGERPRINT_ALGORITHM);
            byte[] der = x509Certificate.getEncoded();
            md.update(der);
            final byte[] digest = md.digest();
            return DatatypeConverter.printHexBinary(digest);
        } catch (NoSuchAlgorithmException e) {
            LOG.warn(String.format("Algorithm [algorithm = %s] is not available.", FINGERPRINT_ALGORITHM));
        }
        return "";
    }

    public BigInteger getSerialNumber() {
        return x509Certificate.getSerialNumber();
    }

    public Date getNotBefore() {
        return x509Certificate.getNotBefore();
    }

    public CertificateType getCertificateType(){
        return certificateType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Certificate that = (Certificate) o;

        return base46EncodeCertificate.equals(that.base46EncodeCertificate);
    }
}
