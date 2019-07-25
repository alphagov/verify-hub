package uk.gov.ida.hub.config.domain;

public class SignatureVerificationCertificate extends Certificate {

    public SignatureVerificationCertificate() {
    }

    public SignatureVerificationCertificate(X509CertificateConfiguration publicKeyConfiguration) {
        this.fullCert = publicKeyConfiguration.getFullCert();
    }

    @Override
    public CertificateType getCertificateType() {
        return CertificateType.SIGNING;
    }
}
