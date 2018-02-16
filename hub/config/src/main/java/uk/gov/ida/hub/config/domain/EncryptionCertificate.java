package uk.gov.ida.hub.config.domain;


public class EncryptionCertificate extends Certificate {

    public EncryptionCertificate() {
    }

    public EncryptionCertificate(X509Certificate publicKeyConfiguration) {
        this.fullCert = publicKeyConfiguration.getCert();
    }

    @Override
    public CertificateType getType() {
        return CertificateType.ENCRYPTION;
    }
}
