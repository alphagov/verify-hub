package uk.gov.ida.hub.config.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import static java.text.MessageFormat.format;
import static uk.gov.ida.common.shared.security.Certificate.BEGIN_CERT;
import static uk.gov.ida.common.shared.security.Certificate.END_CERT;

/* This class is modified from X509CertificateConfiguration in security-utils due to an
* outstanding bug https://github.com/FasterXML/jackson-databind/issues/1358 which means
* we cannot directly deserialize to X509CertificateConfiguration */
public class X509CertificateConfiguration {

    private String fullCertificate;

    @SuppressWarnings("unused")
    @JsonCreator
    public X509CertificateConfiguration(@JsonProperty("cert") @JsonAlias({ "x509", "fullCertificate" }) String cert) {
        this.fullCertificate = format("{0}\n{1}\n{2}", BEGIN_CERT, cert.trim(), END_CERT);
    }

    @JsonIgnore
    public String getFullCert() {
        return fullCertificate;
    }
}
