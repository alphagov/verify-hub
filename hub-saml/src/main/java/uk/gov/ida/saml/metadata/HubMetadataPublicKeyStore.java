package uk.gov.ida.saml.metadata;

import com.google.common.base.Throwables;
import com.google.inject.Inject;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.security.credential.UsageType;
import org.opensaml.xmlsec.signature.X509Certificate;
import org.opensaml.xmlsec.signature.X509Data;
import uk.gov.ida.saml.security.PublicKeyFactory;
import uk.gov.ida.saml.core.InternalPublicKeyStore;
import uk.gov.ida.saml.metadata.exceptions.HubEntityMissingException;

import javax.inject.Named;
import java.security.PublicKey;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @deprecated Use {@link uk.gov.ida.saml.security.MetadataBackedSignatureValidator} instead
 */
@Deprecated
public class HubMetadataPublicKeyStore implements InternalPublicKeyStore {

    private final MetadataResolver metadataResolver;
    private final PublicKeyFactory publicKeyFactory;
    private final String hubEntityId;

    @Inject
    public HubMetadataPublicKeyStore(MetadataResolver metadataResolver,
                                     PublicKeyFactory publicKeyFactory,
                                     @Named("HubEntityId") String hubEntityId) {
        this.metadataResolver = metadataResolver;
        this.publicKeyFactory = publicKeyFactory;
        this.hubEntityId = hubEntityId;
    }

    @Override
    public List<PublicKey> getVerifyingKeysForEntity() {
        try {
            CriteriaSet criteria = new CriteriaSet(new EntityIdCriterion(hubEntityId));
            return Optional.ofNullable(metadataResolver.resolveSingle(criteria))
                    .map(this::getPublicKeys)
                    .orElseThrow(hubMissingException());
        } catch (ResolverException e) {
            throw Throwables.propagate(e);
        }
    }

    private Supplier<HubEntityMissingException> hubMissingException() {
        return () -> new HubEntityMissingException(MessageFormat.format("The HUB entity-id: \"{0}\" could not be found in the metadata. Metadata could be expired, invalid, or missing entities", this.hubEntityId));
    }

    private List<PublicKey> getPublicKeys(EntityDescriptor entityDescriptor) {
        return entityDescriptor
                .getSPSSODescriptor(SAMLConstants.SAML20P_NS)
                .getKeyDescriptors()
                .stream()
                .filter(keyDescriptor -> keyDescriptor.getUse() == UsageType.SIGNING)
                .flatMap(this::getCertificateFromKeyDescriptor)
                .map(publicKeyFactory::create)
                .collect(Collectors.toList());
    }

    private Stream<X509Certificate> getCertificateFromKeyDescriptor(KeyDescriptor keyDescriptor) {
        List<X509Data> x509Datas = keyDescriptor.getKeyInfo().getX509Datas();
        return x509Datas
                .stream()
                .flatMap(x509Data -> x509Data.getX509Certificates().stream());
    }

}
