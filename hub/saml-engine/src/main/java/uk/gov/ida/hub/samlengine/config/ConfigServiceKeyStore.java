package uk.gov.ida.hub.samlengine.config;

import uk.gov.ida.common.shared.security.X509CertificateFactory;
import uk.gov.ida.common.shared.security.verification.CertificateChainValidator;
import uk.gov.ida.common.shared.security.verification.CertificateValidity;
import uk.gov.ida.common.shared.security.verification.exceptions.CertificateChainValidationException;
import uk.gov.ida.hub.samlengine.domain.CertificateDto;
import uk.gov.ida.hub.samlengine.domain.MatchingServiceConfigEntityDataDto;

import javax.inject.Inject;
import java.net.URI;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.text.MessageFormat.format;

public class ConfigServiceKeyStore {

    private final ConfigProxy configProxy;
    private final CertificateChainValidator certificateChainValidator;
    private final TrustStoreForCertificateProvider trustStoreForCertificateProvider;
    private final X509CertificateFactory x509CertificateFactory;
    private final MatchingServiceAdapterMetadataRetriever matchingServiceAdapterMetadataRetriever;

    @Inject
    public ConfigServiceKeyStore(
            ConfigProxy configProxy,
            CertificateChainValidator certificateChainValidator,
            TrustStoreForCertificateProvider trustStoreForCertificateProvider,
            X509CertificateFactory x509CertificateFactory,
            MatchingServiceAdapterMetadataRetriever matchingServiceAdapterMetadataRetriever) {

        this.configProxy = configProxy;
        this.certificateChainValidator = certificateChainValidator;
        this.trustStoreForCertificateProvider = trustStoreForCertificateProvider;
        this.x509CertificateFactory = x509CertificateFactory;
        this.matchingServiceAdapterMetadataRetriever = matchingServiceAdapterMetadataRetriever;
    }

    public List<PublicKey> getVerifyingKeysForEntity(String entityId) {

        final Optional<MatchingServiceConfigEntityDataDto> msaConfiguration = configProxy.getMsaConfiguration(entityId);
        if(msaConfiguration.isPresent() && msaConfiguration.get().getReadMetadataFromEntityId()) {
            // it's an MSA! it has metadata we can read! so get the certs from the metadata
            return matchingServiceAdapterMetadataRetriever.getPublicSigningKeysForMSA(entityId);
        } else {
            // i.e. it's not an MSA, and RP metadata is enabled
            if(!msaConfiguration.isPresent() && configProxy.getRPMetadataEnabled(entityId)) {
                return matchingServiceAdapterMetadataRetriever.getPublicSigningKeysForRP(entityId);
            } else {
                final Collection<CertificateDto> certificates = configProxy.getSignatureVerificationCertificates(entityId);
                List<PublicKey> publicKeys = new ArrayList<>();
                for (CertificateDto keyFromConfig : certificates) {
                    String base64EncodedCertificateValue = keyFromConfig.getCertificate();
                    final X509Certificate certificate = x509CertificateFactory.createCertificate(base64EncodedCertificateValue);
                    KeyStore trustStore = trustStoreForCertificateProvider.getTrustStoreFor(keyFromConfig.getFederationEntityType());
                    validate(certificate, trustStore);
                    publicKeys.add(certificate.getPublicKey());
                }
                return publicKeys;
            }
        }
    }

    public PublicKey getEncryptionKeyForEntity(String entityId) {

        final Optional<MatchingServiceConfigEntityDataDto> msaConfiguration = configProxy.getMsaConfiguration(entityId);

        if(msaConfiguration.isPresent() && msaConfiguration.get().getReadMetadataFromEntityId()) {
            // it's an MSA! it has metadata we can read! so get the cert from the metadata
            return matchingServiceAdapterMetadataRetriever.getPublicEncryptionKeyForMSA(entityId);
        } else {
            // i.e. it's not an MSA, and RP metadata is enabled
            if(!msaConfiguration.isPresent() && configProxy.getRPMetadataEnabled(entityId)) {
                return matchingServiceAdapterMetadataRetriever.getPublicEncryptionKeyForRP(entityId);
            } else {
                final CertificateDto certificateDto = configProxy.getEncryptionCertificate(entityId);
                String base64EncodedCertificateValue = certificateDto.getCertificate();
                X509Certificate certificate = x509CertificateFactory.createCertificate(base64EncodedCertificateValue);
                KeyStore trustStore = trustStoreForCertificateProvider.getTrustStoreFor(certificateDto.getFederationEntityType());
                validate(certificate, trustStore);
                return certificate.getPublicKey();
            }
        }
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
