package uk.gov.ida.hub.config.domain;


import uk.gov.ida.common.shared.configuration.DeserializablePublicKeyConfiguration;

public class EncryptionCertificate extends Certificate {

    public EncryptionCertificate() {
    }

    public EncryptionCertificate(DeserializablePublicKeyConfiguration publicKeyConfiguration) {
        this.fullCert = publicKeyConfiguration.getCert();
    }

    @Override
    public CertificateType getType() {
        return CertificateType.ENCRYPTION;
    }
}
