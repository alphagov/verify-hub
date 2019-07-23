package uk.gov.ida.hub.config.domain.builders;

import uk.gov.ida.hub.config.domain.SignatureVerificationCertificate;

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
        return new SignatureVerificationCertificate(x509);
    }

    public SignatureVerificationCertificateBuilder withX509(String x509) {
        this.x509 = x509;
        return this;
    }
}
