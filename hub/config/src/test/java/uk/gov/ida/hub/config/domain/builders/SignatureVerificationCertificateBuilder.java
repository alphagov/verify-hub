package uk.gov.ida.hub.config.domain.builders;

import uk.gov.ida.hub.config.domain.SignatureVerificationCertificate;
import uk.gov.ida.hub.config.domain.X509CertificateConfiguration;

import static java.text.MessageFormat.format;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.ida.common.shared.security.Certificate.BEGIN_CERT;
import static uk.gov.ida.common.shared.security.Certificate.END_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PUBLIC_SIGNING_CERT;

public class SignatureVerificationCertificateBuilder {

    private String x509 = HUB_TEST_PUBLIC_SIGNING_CERT;

    public static SignatureVerificationCertificateBuilder aSignatureVerificationCertificate() {
        return new SignatureVerificationCertificateBuilder();
    }

    public SignatureVerificationCertificate build() {
        String fullCert = format("{0}\n{1}\n{2}", BEGIN_CERT, x509.trim(), END_CERT);
        X509CertificateConfiguration configuration = mock(X509CertificateConfiguration.class);
        when(configuration.getFullCert()).thenReturn(fullCert);
        return new SignatureVerificationCertificate(configuration);
    }

    public SignatureVerificationCertificateBuilder withX509(String x509) {
        this.x509 = x509;
        return this;
    }
}
