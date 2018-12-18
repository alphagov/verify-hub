package uk.gov.ida.hub.samlsoapproxy.config;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import org.apache.log4j.Logger;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.criterion.EntityRoleCriterion;
import org.opensaml.saml.criterion.ProtocolCriterion;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.metadata.AttributeAuthorityDescriptor;
import org.opensaml.saml.security.impl.MetadataCredentialResolver;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.criteria.UsageCriterion;
import org.opensaml.security.x509.X509Credential;
import uk.gov.ida.common.shared.security.verification.CertificateChainValidator;
import uk.gov.ida.common.shared.security.verification.CertificateValidity;
import uk.gov.ida.common.shared.security.verification.exceptions.CertificateChainValidationException;
import uk.gov.ida.hub.samlsoapproxy.domain.FederationEntityType;
import uk.gov.ida.hub.samlsoapproxy.exceptions.CouldNotGetMSACertsException;
import uk.gov.ida.hub.samlsoapproxy.exceptions.EncryptionKeyExtractionException;
import uk.gov.ida.hub.samlsoapproxy.exceptions.SigningKeyExtractionException;
import uk.gov.ida.saml.metadata.factories.DropwizardMetadataResolverFactory;

import javax.inject.Inject;
import javax.ws.rs.client.ClientBuilder;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.text.MessageFormat.format;

public class MatchingServiceAdapterMetadataRetriever {

    private static final Logger LOG = Logger.getLogger(MatchingServiceAdapterMetadataRetriever.class);

    private static final boolean VALIDATE_METADATA_SIGNATURES = true;

    private final CertificateChainValidator certificateChainValidator;
    private final KeyStore msTrustStore;
    private final DropwizardMetadataResolverFactory dropwizardMetadataResolverFactory;

    private final Map<String, MetadataCredentialResolver> resolvers = new HashMap<>();

    @Inject
    public MatchingServiceAdapterMetadataRetriever(TrustStoreForCertificateProvider trustStoreForCertificateProvider, CertificateChainValidator certificateChainValidator, DropwizardMetadataResolverFactory dropwizardMetadataResolverFactory) {
        this.certificateChainValidator = certificateChainValidator;

        this.msTrustStore = trustStoreForCertificateProvider.getTrustStoreFor(FederationEntityType.MS);
        this.dropwizardMetadataResolverFactory = dropwizardMetadataResolverFactory;
    }

    public List<PublicKey> getPublicSigningKeysForMSA(String entityId) {
        final MetadataCredentialResolver metadataResolverForMSA = getMetadataCredentialResolverForMSA(entityId);

        List<PublicKey> publicKeys = new ArrayList<>();
        final CriteriaSet criteriaSet = new CriteriaSet(
                new EntityIdCriterion(entityId),
                new UsageCriterion(UsageType.SIGNING),
                new ProtocolCriterion(SAMLConstants.SAML20P_NS),
                new EntityRoleCriterion(AttributeAuthorityDescriptor.DEFAULT_ELEMENT_NAME)
        );
        try {
            Credential credential = metadataResolverForMSA.resolveSingle(criteriaSet);
            if (credential instanceof X509Credential) {
                // important that this validation is done because it's signed by the MSA and we've not checked it before

                // note validate() throws a runtime exception if it's not valid
                validate(((X509Credential) credential).getEntityCertificate(), msTrustStore);
                publicKeys.add(credential.getPublicKey());
            }
        } catch (ResolverException e) {
            throw new SigningKeyExtractionException("Unable to resolve metadata.", e);
        }
        LOG.info(format("found {0} signing certs for {1}", publicKeys.size(), entityId));
        if(publicKeys.size()==0) {
            throw new SigningKeyExtractionException("did not find any signing certs in metadata");
        }
        return publicKeys;
    }

    public PublicKey getPublicEncryptionKeyForMSA(String entityId) {
        final MetadataCredentialResolver metadataResolverForMSA = getMetadataCredentialResolverForMSA(entityId);

        List<PublicKey> publicKeys = new ArrayList<>();
        final CriteriaSet criteriaSet = new CriteriaSet(
                new EntityIdCriterion(entityId),
                new UsageCriterion(UsageType.ENCRYPTION),
                new ProtocolCriterion(SAMLConstants.SAML20P_NS),
                new EntityRoleCriterion(AttributeAuthorityDescriptor.DEFAULT_ELEMENT_NAME)
        );
        try {
            Credential credential = metadataResolverForMSA.resolveSingle(criteriaSet);
            if (credential instanceof X509Credential) {
                // important that this validation is done because it's signed by the MSA and we've not checked it before

                // note validate() throws a runtime exception if it's not valid
                validate(((X509Credential) credential).getEntityCertificate(), msTrustStore);
                publicKeys.add(credential.getPublicKey());
            }
        } catch (ResolverException e) {
            throw new EncryptionKeyExtractionException("Unable to resolve metadata.", e);
        }
        LOG.info(format("found {0} encryption certs for {1}", publicKeys.size(), entityId));
        return publicKeys.stream().findFirst().orElseThrow(EncryptionKeyExtractionException::noKeyFound);
    }

    private MetadataCredentialResolver getMetadataCredentialResolverForMSA(String entityId) {
        resolvers.computeIfAbsent(entityId, f -> {
            try {
                LOG.info(format("creating metadata resolver for {0}", entityId));
                MetadataResolver metadataResolver = dropwizardMetadataResolverFactory
                        .createMetadataResolverWithClient(MSAMetadataResolverConfigurationBuilder.aConfig()
                                        .withMsaEntityId(entityId)
                                        .withUri(entityId)
                                        .withTrustStore(msTrustStore)
                                        .withHubFederationId("")//this is not set in the generated metadata on MSAs
                                        .build(),
                                VALIDATE_METADATA_SIGNATURES,
                                ClientBuilder.newClient());

                return new MetadataCredentialResolverInitializer(metadataResolver).initialize();
            } catch (ComponentInitializationException e) {
                throw new CouldNotGetMSACertsException(e);
            }
        });
        LOG.info(format("using metadata resolver for {0}", entityId));
        return resolvers.get(entityId);
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
