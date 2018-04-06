package uk.gov.ida.saml.metadata;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import org.apache.xml.security.exceptions.Base64DecodingException;
import org.apache.xml.security.utils.Base64;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.security.credential.UsageType;
import org.opensaml.xmlsec.signature.X509Certificate;
import uk.gov.ida.saml.security.SigningKeyStore;
import uk.gov.ida.saml.metadata.exceptions.NoKeyConfiguredForEntityException;

import java.io.ByteArrayInputStream;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Throwables.propagate;


/**
 * @deprecated Use {@link uk.gov.ida.saml.security.MetadataBackedSignatureValidator} instead
 */
@Deprecated
public class IdpMetadataPublicKeyStore implements SigningKeyStore {

    private final MetadataResolver metadataResolver;

    @Inject
    public IdpMetadataPublicKeyStore(MetadataResolver metadataResolver) {
        this.metadataResolver = metadataResolver;
    }

    @Override
    public List<PublicKey> getVerifyingKeysForEntity(String entityId) {
        Optional<EntityDescriptor> entityDescriptor = getEntityDescriptor(entityId);
        if (entityDescriptor.isPresent()) {
            final List<PublicKey> publicKeys = getPublicKeys(entityDescriptor.get(), UsageType.SIGNING);
            if (!publicKeys.isEmpty()) {
                return publicKeys;
            }
        }
        throw new NoKeyConfiguredForEntityException(entityId);
    }

    private List<PublicKey> getPublicKeys(EntityDescriptor entityDescriptor, UsageType keyType) {
        return Optional.ofNullable(entityDescriptor.getIDPSSODescriptor(SAMLConstants.SAML20P_NS))
                .map(descriptor -> getPublicKeys(descriptor, keyType))
                .orElse(Collections.emptyList());
    }

    private List<PublicKey> getPublicKeys(IDPSSODescriptor descriptor, UsageType keyType) {
        return descriptor.getKeyDescriptors().stream()
                .filter(keyDescriptor -> keyDescriptor.getUse().equals(keyType))
                .flatMap(this::getPublicKeys)
                .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
    }

    private Stream<PublicKey> getPublicKeys(KeyDescriptor keyDescriptor) {
        return keyDescriptor.getKeyInfo().getX509Datas().stream()
                .flatMap(x -> x.getX509Certificates().stream())
                .map(this::getPublicKey);
    }

    private PublicKey getPublicKey(X509Certificate x509Certificate) {
        try {
            byte[] derValue = Base64.decode(x509Certificate.getValue());
            CertificateFactory certificateFactory =
                    CertificateFactory
                            .getInstance("X.509");
            Certificate certificate = certificateFactory.generateCertificate(new ByteArrayInputStream(derValue));
            return certificate.getPublicKey();
        } catch (Base64DecodingException | CertificateException e) {
            throw propagate(e);
        }
    }

    private Optional<EntityDescriptor> getEntityDescriptor(String entityId) {
        try {
            CriteriaSet criteria = new CriteriaSet(new EntityIdCriterion(entityId));
            return Optional.ofNullable(metadataResolver.resolveSingle(criteria));
        } catch (ResolverException e) {
            throw propagate(e);
        }
    }
}
