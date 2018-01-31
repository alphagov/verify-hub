package uk.gov.ida.hub.config.domain.builders;

import uk.gov.ida.hub.config.domain.EncryptionCertificate;

import static java.text.MessageFormat.format;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PUBLIC_ENCRYPTION_CERT;

public class EncryptionCertificateBuilder {
    private String x509 = HUB_TEST_PUBLIC_ENCRYPTION_CERT;

    public static EncryptionCertificateBuilder anEncryptionCertificate() {
        return new EncryptionCertificateBuilder();
    }

    public EncryptionCertificate build() {
        String fullCert = format("-----BEGIN CERTIFICATE-----\n{0}\n-----END CERTIFICATE-----", x509.trim());
        return new TestEncryptionCertificate(fullCert);
    }

    public EncryptionCertificateBuilder withX509(String x509Value) {
        this.x509 = x509Value;
        return this;
    }

    private class TestEncryptionCertificate extends EncryptionCertificate {
        TestEncryptionCertificate(String cert) {
            this.fullCert = cert;
        }

    }
}
