package uk.gov.ida.hub.config.domain;

import uk.gov.ida.hub.config.dto.FederationEntityType;

import java.util.Collection;
import java.util.List;

public interface CertificateConfigurable<T> extends EntityIdentifiable {

    boolean isSelfService();
    EncryptionCertificate getEncryptionCertificate();
    Collection<SignatureVerificationCertificate> getSignatureVerificationCertificates();

    default boolean isEnabled() {
        return true;
    }

    T override(List<X509CertificateConfiguration> signatureVerificationCertificateList, X509CertificateConfiguration encryptionCertificate);

    FederationEntityType getEntityType();
}