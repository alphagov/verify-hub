package uk.gov.ida.hub.config.domain;

public class EncryptionCertificate extends Certificate {

    public EncryptionCertificate() {
    }

    public EncryptionCertificate(String publicKey) {
        this.cert = publicKey;
    }

    @Override
    public CertificateType getCertificateType() {
        return CertificateType.ENCRYPTION;
    }
}
