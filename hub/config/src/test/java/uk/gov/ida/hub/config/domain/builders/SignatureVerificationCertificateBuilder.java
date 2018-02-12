package uk.gov.ida.hub.config.domain.builders;

import uk.gov.ida.common.shared.configuration.DeserializablePublicKeyConfiguration;
import uk.gov.ida.hub.config.domain.SignatureVerificationCertificate;
import uk.gov.ida.hub.config.domain.X509Certificate;

import static java.text.MessageFormat.format;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PUBLIC_SIGNING_CERT;

public class SignatureVerificationCertificateBuilder {

    private String x509Value = HUB_TEST_PUBLIC_SIGNING_CERT;

    public static SignatureVerificationCertificateBuilder aSignatureVerificationCertificate() {
        return new SignatureVerificationCertificateBuilder();
    }

    public SignatureVerificationCertificate build() {
        String fullCert = format("-----BEGIN CERTIFICATE-----\n{0}\n-----END CERTIFICATE-----", x509Value.trim());
        X509Certificate configuration = mock(X509Certificate.class);
        when(configuration.getCert()).thenReturn(fullCert);
        return new SignatureVerificationCertificate(configuration);
    }

    public SignatureVerificationCertificateBuilder withX509(String x509Value) {
        this.x509Value = x509Value;
        return this;
    }
}
