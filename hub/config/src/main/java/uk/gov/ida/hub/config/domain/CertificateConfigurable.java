package uk.gov.ida.hub.config.domain;

import uk.gov.ida.hub.config.dto.FederationEntityType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface CertificateConfigurable<T> extends EntityIdentifiable {

    boolean isSelfService();
    Certificate getEncryptionCertificate();
    Collection<Certificate> getSignatureVerificationCertificates();

    default boolean isEnabled() {
        return true;
    }

    T override(List<String> signatureVerificationCertificateList, String encryptionCertificate, CertificateOrigin certificateOrigin);

    FederationEntityType getEntityType();

    default Collection<Certificate> getAllCertificates(){
        List<Certificate> certs = new ArrayList();
        certs.add(getEncryptionCertificate());
        certs.addAll(getSignatureVerificationCertificates());
        return certs;
    }
}