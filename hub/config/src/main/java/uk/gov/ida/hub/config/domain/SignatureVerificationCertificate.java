package uk.gov.ida.hub.config.domain;

public class SignatureVerificationCertificate extends Certificate {

    public SignatureVerificationCertificate() {
    }

    public SignatureVerificationCertificate(String publicKey) {
        this.cert = publicKey;
    }

    @Override
    public CertificateType getCertificateType() {
        return CertificateType.SIGNING;
    }
}
