package uk.gov.ida.hub.config.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import uk.gov.ida.common.shared.configuration.EncodedCertificateConfiguration;
import uk.gov.ida.common.shared.configuration.PublicKeyFileConfiguration;
import uk.gov.ida.common.shared.configuration.X509CertificateConfiguration;

import java.io.ByteArrayInputStream;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import static java.text.MessageFormat.format;
import static uk.gov.ida.common.shared.security.Certificate.BEGIN_CERT;
import static uk.gov.ida.common.shared.security.Certificate.END_CERT;

/* This class is modified from X509CertificateConfiguration in security-utils due to an
* outstanding bug https://github.com/FasterXML/jackson-databind/issues/1358 which means
* we cannot directly deserialize to X509CertificateConfiguration */
public class X509Certificate {

    private String fullCertificate;
    private Certificate certificate;

    @SuppressWarnings("unused")
    @JsonCreator
    public X509Certificate(@JsonProperty("cert") @JsonAlias({ "x509", "fullCertificate" }) String cert) {
        this.fullCertificate = format("{0}\n{1}\n{2}", BEGIN_CERT, cert.trim(), END_CERT);
    }

    public String getCert() {
        return fullCertificate;
    }

    protected static Certificate getCertificateFromString(String cert) {
        try {
            return CertificateFactory.getInstance("X509").generateCertificate(
                    new ByteArrayInputStream(cert.getBytes())
            );
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }
    }
}
