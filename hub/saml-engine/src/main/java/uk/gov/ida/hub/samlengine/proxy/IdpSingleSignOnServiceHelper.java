package uk.gov.ida.hub.samlengine.proxy;

import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import org.apache.log4j.Logger;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.SingleSignOnService;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.exceptions.ApplicationException;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import static java.text.MessageFormat.format;

public class IdpSingleSignOnServiceHelper {

    private static final Logger LOG = Logger.getLogger(IdpSingleSignOnServiceHelper.class);

    private final MetadataResolver metadataProvider;

    @Inject
    public IdpSingleSignOnServiceHelper(@Named("VerifyMetadataResolver") MetadataResolver metadataProvider) {
        this.metadataProvider = metadataProvider;
    }

    public URI getSingleSignOn(String entityId) {
        EntityDescriptor idpEntityDescriptor;
        try {
            CriteriaSet criteria = new CriteriaSet(new EntityIdCriterion(entityId));
            idpEntityDescriptor = metadataProvider.resolveSingle(criteria);
        } catch (ResolverException e) {
            LOG.error(format("Exception when accessing metadata: {0}", e));
            throw new RuntimeException(e);
        }

        if(idpEntityDescriptor!=null) {
            final IDPSSODescriptor idpssoDescriptor = idpEntityDescriptor.getIDPSSODescriptor(SAMLConstants.SAML20P_NS);
            final List<SingleSignOnService> singleSignOnServices = idpssoDescriptor.getSingleSignOnServices();
            if (singleSignOnServices.isEmpty()) {
                LOG.error(format("No singleSignOnServices present for IDP entityId: {0}", entityId));
            } else {
                if (singleSignOnServices.size() > 1) {
                    LOG.warn(format("More than one singleSignOnService present: {0} for {1}", singleSignOnServices.size(), entityId));
                }
                return URI.create(singleSignOnServices.get(0).getLocation());
            }
        }

        throw ApplicationException.createUnauditedException(ExceptionType.NOT_FOUND, UUID.randomUUID(), new RuntimeException(format("no entity descriptor for IDP: {0}", entityId)));

    }

}
