package uk.gov.ida.hub.samlengine.proxy;

import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import org.apache.log4j.Logger;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.SingleSignOnService;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.hub.samlengine.EidasMetadataResolver;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import java.net.URI;
import java.util.List;
import java.util.Timer;
import java.util.UUID;

import static com.google.common.base.Throwables.propagate;
import static java.text.MessageFormat.format;

public class CountrySingleSignOnServiceHelper {

    private static final Logger LOG = Logger.getLogger(CountrySingleSignOnServiceHelper.class);

    private final Client client;

    @Inject
    public CountrySingleSignOnServiceHelper(Client client) {
        this.client = client;
    }

    public URI getSingleSignOn(String entityId) {
        EidasMetadataResolver metadataResolver = new EidasMetadataResolver(new Timer(), client, URI.create(entityId));

        try {
            EntityDescriptor idpEntityDescriptor;
            try {
                CriteriaSet criteria = new CriteriaSet(new EntityIdCriterion(entityId));
                idpEntityDescriptor = metadataResolver.resolveSingle(criteria);
            } catch (ResolverException e) {
                LOG.error(format("Exception when accessing metadata: {0}", e));
                throw propagate(e);
            }

            if (idpEntityDescriptor != null) {
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
        } finally {
            if (metadataResolver != null) {
                metadataResolver.destroy();
            }
        }
    }

}
