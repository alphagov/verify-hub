package uk.gov.ida.hub.config.domain;

public class EncryptionCertificate extends Certificate {

    public EncryptionCertificate() {
    }

    public EncryptionCertificate(X509CertificateConfiguration publicKeyConfiguration) {
        this.fullCert = publicKeyConfiguration.getFullCert();
    }

    @Override
    public CertificateType getCertificateType() {
        return CertificateType.ENCRYPTION;
    }
}
