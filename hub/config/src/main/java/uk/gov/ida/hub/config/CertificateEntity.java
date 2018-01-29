package uk.gov.ida.hub.config;

import uk.gov.ida.hub.config.domain.EncryptionCertificate;
import uk.gov.ida.hub.config.domain.SignatureVerificationCertificate;

import java.util.Collection;

public interface CertificateEntity {

    EncryptionCertificate getEncryptionCertificate();

    Collection<SignatureVerificationCertificate> getSignatureVerificationCertificates();

    default Boolean isEnabled() {
        return true;
    }
}
