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
import java.util.Objects;
import java.util.Optional;

public class Certificate {
    private static final Logger LOG = LoggerFactory.getLogger(Certificate.class);
    private static final String FINGERPRINT_ALGORITHM = "SHA-256";

    private final String issuerEntityId;
    private final FederationEntityType federationEntityType;
    private final String base64EncodedCertificate;
    private final CertificateUse certificateUse;
    private CertificateOrigin certificateOrigin;
    private final boolean enabled;
    private final X509Certificate x509Certificate;
    private final boolean valid;

    public Certificate(String issuerEntityId, FederationEntityType federationEntityType, String base64EncodedCertificate, CertificateUse certificateUse, CertificateOrigin certificateOrigin, boolean enabled){
        this.issuerEntityId = issuerEntityId;
        this.federationEntityType = federationEntityType;
        this.base64EncodedCertificate = base64EncodedCertificate;
        this.certificateUse = certificateUse;
        this.certificateOrigin = certificateOrigin;
        this.enabled = enabled;
        this.x509Certificate = decodeBase64(base64EncodedCertificate);
        this.valid = x509Certificate != null;
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

    public Optional<String> getBase64Encoded() {
        return getX509Certificate().map(x509 -> {
            try {
                return Base64.getEncoder().encodeToString(x509.getEncoded());
            } catch (CertificateEncodingException e) {
                //this should never happen as the x509 should be valid.
                return null;
            }
        });
    }

    public Optional<X509Certificate> getX509Certificate() {
        return Optional.ofNullable(x509Certificate);
    }

    private X509Certificate decodeBase64(String base64EncodedCertificate) {
        if (base64EncodedCertificate == null || base64EncodedCertificate.isBlank()){
            return null;
        }

        String cleanUpRegex = "(-----BEGIN CERTIFICATE-----)|(\\n)|(-----END CERTIFICATE-----)";
        String clean64 = base64EncodedCertificate.replaceAll(cleanUpRegex, "");
        try {
            byte[] certBytes = Base64.getDecoder().decode(clean64);
            return (X509Certificate) CertificateFactory
                    .getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(certBytes));
        } catch (IllegalArgumentException | CertificateException e) {
            //Any problems creating the certificate should return null.
            return null;
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

    public CertificateUse getCertificateUse(){
        return certificateUse;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Certificate that = (Certificate) o;
        return issuerEntityId.equals(that.issuerEntityId) &&
                base64EncodedCertificate.equals(that.base64EncodedCertificate) &&
                certificateUse == that.certificateUse;
    }

    @Override
    public int hashCode() {
        return Objects.hash(issuerEntityId, base64EncodedCertificate, certificateUse);
    }
}
