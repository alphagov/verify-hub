package uk.gov.ida.hub.samlengine.metadata;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.criterion.EntityRoleCriterion;
import org.opensaml.saml.criterion.ProtocolCriterion;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml.security.impl.MetadataCredentialResolver;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.credential.criteria.impl.EvaluablePublicKeyCredentialCriterion;
import org.opensaml.security.criteria.UsageCriterion;
import org.opensaml.security.x509.X509Credential;
import uk.gov.ida.hub.samlengine.exceptions.SigningKeyExtractionException;

import javax.inject.Inject;
import javax.inject.Named;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import static uk.gov.ida.hub.samlengine.SamlEngineModule.VERIFY_METADATA_RESOLVER;

public class SigningCertFromMetadataExtractor {

    private final MetadataCredentialResolver credentialResolver;
    private final String hubEntityId;

    @Inject
    public SigningCertFromMetadataExtractor(@Named(VERIFY_METADATA_RESOLVER) MetadataResolver metadataResolver,
                                            @Named("HubEntityId") String hubEntityId) throws ComponentInitializationException {
        this.credentialResolver = new MetadataCredentialResolverInitializer(metadataResolver).initialize();
        this.hubEntityId = hubEntityId;
    }

    public X509Certificate getSigningCertForCurrentSigningKey(PublicKey publicSigningKey) {
        CriteriaSet criteriaSet = new CriteriaSet(
            new EntityIdCriterion(hubEntityId),
            new UsageCriterion(UsageType.SIGNING),
            new ProtocolCriterion(SAMLConstants.SAML20P_NS),
            new EntityRoleCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME),
            new EvaluablePublicKeyCredentialCriterion(publicSigningKey)
        );
        try {
            Credential credential = credentialResolver.resolveSingle(criteriaSet);
            if (credential instanceof X509Credential) {
                X509Certificate x509Cert = ((X509Credential)credential).getEntityCertificate();
                if (x509Cert.getPublicKey().equals(publicSigningKey)){
                    return x509Cert;
                }
            }
        } catch (ResolverException e) {
            throw new SigningKeyExtractionException("Unable to resolve metadata.", e);
        }
        throw new SigningKeyExtractionException("Certificate for public signing key not found in metadata.");
    }
}
