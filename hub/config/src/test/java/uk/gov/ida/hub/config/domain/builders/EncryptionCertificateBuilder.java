package uk.gov.ida.hub.config.domain.builders;

import uk.gov.ida.hub.config.domain.EncryptionCertificate;
import uk.gov.ida.hub.config.domain.X509CertificateConfiguration;

import static java.text.MessageFormat.format;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.ida.common.shared.security.Certificate.BEGIN_CERT;
import static uk.gov.ida.common.shared.security.Certificate.END_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PUBLIC_ENCRYPTION_CERT;

public class EncryptionCertificateBuilder {
    private String x509 = HUB_TEST_PUBLIC_ENCRYPTION_CERT;

    public static EncryptionCertificateBuilder anEncryptionCertificate() {
        return new EncryptionCertificateBuilder();
    }

    public EncryptionCertificateBuilder withX509(String x509) {
        this.x509 = x509;
        return this;
    }

    public EncryptionCertificate build() {
        String fullCert = format("{0}\n{1}\n{2}", BEGIN_CERT, x509.trim(), END_CERT);
        X509CertificateConfiguration configuration = mock(X509CertificateConfiguration.class);
        when(configuration.getFullCert()).thenReturn(fullCert);
        return new EncryptionCertificate(configuration);
    }
}
