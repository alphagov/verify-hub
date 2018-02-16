package uk.gov.ida.hub.config.domain;

public class SignatureVerificationCertificate extends Certificate {

    public SignatureVerificationCertificate() {
    }

    public SignatureVerificationCertificate(X509Certificate publicKeyConfiguration) {
        this.fullCert = publicKeyConfiguration.getCert();
    }

    @Override
    public CertificateType getType() {
        return CertificateType.SIGNING;
    }
}
