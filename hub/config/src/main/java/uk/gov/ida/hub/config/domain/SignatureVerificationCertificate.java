package uk.gov.ida.hub.config.domain;

import uk.gov.ida.common.shared.configuration.DeserializablePublicKeyConfiguration;

public class SignatureVerificationCertificate extends Certificate {

    public SignatureVerificationCertificate() {
    }

    public SignatureVerificationCertificate(DeserializablePublicKeyConfiguration publicKeyConfiguration) {
        this.fullCert = publicKeyConfiguration.getCert();
    }

    @Override
    public CertificateType getCertificateType() {
        return CertificateType.SIGNING;
    }
}
