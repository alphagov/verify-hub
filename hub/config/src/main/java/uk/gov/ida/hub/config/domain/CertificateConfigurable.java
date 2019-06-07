package uk.gov.ida.hub.config.domain;

import java.util.Collection;

public interface CertificateConfigurable extends EntityIdentifiable {

    EncryptionCertificate getEncryptionCertificate();

    Collection<SignatureVerificationCertificate> getSignatureVerificationCertificates();

    default Boolean isEnabled() {
        return true;
    }
}
