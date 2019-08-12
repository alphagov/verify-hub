package uk.gov.ida.hub.samlsoapproxy.config;

import uk.gov.ida.common.shared.security.X509CertificateFactory;
import uk.gov.ida.common.shared.security.verification.CertificateChainValidator;
import uk.gov.ida.common.shared.security.verification.CertificateValidity;
import uk.gov.ida.common.shared.security.verification.exceptions.CertificateChainValidationException;
import uk.gov.ida.hub.samlsoapproxy.domain.CertificateDto;

import javax.inject.Inject;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.text.MessageFormat.format;

public class ConfigServiceKeyStore {

    private final CertificatesConfigProxy certificatesConfigProxy;
    private final CertificateChainValidator certificateChainValidator;
    private final TrustStoreForCertificateProvider trustStoreForCertificateProvider;
    private final X509CertificateFactory x509CertificateFactory;

    @Inject
    public ConfigServiceKeyStore(
            CertificatesConfigProxy certificatesConfigProxy,
            CertificateChainValidator certificateChainValidator,
            TrustStoreForCertificateProvider trustStoreForCertificateProvider,
            X509CertificateFactory x509CertificateFactory) {

        this.certificatesConfigProxy = certificatesConfigProxy;
        this.certificateChainValidator = certificateChainValidator;
        this.trustStoreForCertificateProvider = trustStoreForCertificateProvider;
        this.x509CertificateFactory = x509CertificateFactory;
    }

    public List<PublicKey> getVerifyingKeysForEntity(String entityId) {
        Collection<CertificateDto> certificates = certificatesConfigProxy.getSignatureVerificationCertificates(entityId);
        List<PublicKey> publicKeys = new ArrayList<>();
        for (CertificateDto keyFromConfig : certificates) {
            String base64EncodedCertificateValue = keyFromConfig.getCertificate();
            final X509Certificate certificate = x509CertificateFactory.createCertificate(base64EncodedCertificateValue);
            trustStoreForCertificateProvider.getTrustStoreFor(keyFromConfig.getFederationEntityType())
                    .ifPresent(keyStore -> validate(certificate, keyStore));
            publicKeys.add(certificate.getPublicKey());
        }

        return publicKeys;
    }

    public PublicKey getEncryptionKeyForEntity(String entityId) {
        CertificateDto certificateDto = certificatesConfigProxy.getEncryptionCertificate(entityId);
        String base64EncodedCertificateValue = certificateDto.getCertificate();
        final X509Certificate certificate = x509CertificateFactory.createCertificate(base64EncodedCertificateValue);
        trustStoreForCertificateProvider.getTrustStoreFor(certificateDto.getFederationEntityType())
                .ifPresent(keyStore -> validate(certificate, keyStore));

        return certificate.getPublicKey();
    }

    private void validate(final X509Certificate certificate, final KeyStore trustStore) {
        CertificateValidity certificateValidity = certificateChainValidator.validate(certificate, trustStore);
        if (!certificateValidity.isValid()) {
            throw new CertificateChainValidationException(
                    format("Certificate is not valid: {0}", getDnForCertificate(certificate)),
                    certificateValidity.getException().get());
        }
    }

    private String getDnForCertificate(X509Certificate certificate) {
        if (certificate != null && certificate.getSubjectDN() != null) {
            return certificate.getSubjectDN().getName();
        }
        return "Unable to get DN";
    }
}
