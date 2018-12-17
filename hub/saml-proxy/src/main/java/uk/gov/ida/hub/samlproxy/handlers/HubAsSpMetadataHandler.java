package uk.gov.ida.hub.samlproxy.handlers;

import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import org.joda.time.DateTime;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import uk.gov.ida.hub.samlproxy.SamlProxyConfiguration;
import uk.gov.ida.hub.samlproxy.exceptions.HubEntityNotFoundException;
import uk.gov.ida.saml.deserializers.StringToOpenSamlObjectTransformer;
import uk.gov.ida.saml.serializers.XmlObjectToBase64EncodedStringTransformer;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;

public class HubAsSpMetadataHandler {

    private final SamlProxyConfiguration samlProxyConfiguration;
    private final MetadataResolver metadataResolver;
    private final XmlObjectToBase64EncodedStringTransformer<EntityDescriptor> entityDescriptorElementTransformer;
    private final StringToOpenSamlObjectTransformer<EntityDescriptor> elementEntityDescriptorTransformer;
    private final String hubEntityId;

    @Inject
    public HubAsSpMetadataHandler(
            MetadataResolver metadataResolver,
            SamlProxyConfiguration samlProxyConfiguration,
            XmlObjectToBase64EncodedStringTransformer<EntityDescriptor> entityDescriptorElementTransformer,
            StringToOpenSamlObjectTransformer<EntityDescriptor> elementEntityDescriptorTransformer,
            @Named("HubEntityId") String hubEntityId) {
        this.metadataResolver = metadataResolver;
        this.samlProxyConfiguration = samlProxyConfiguration;
        this.entityDescriptorElementTransformer = entityDescriptorElementTransformer;
        this.elementEntityDescriptorTransformer = elementEntityDescriptorTransformer;
        this.hubEntityId = hubEntityId;
    }


    public EntityDescriptor getMetadataAsAServiceProvider() {
        try {
            CriteriaSet criteria = new CriteriaSet(new EntityIdCriterion(hubEntityId));
            return Optional.ofNullable(metadataResolver.resolveSingle(criteria))
                    .map(this::copyEntityDescriptor)
                    .map(this::addValidUntilTime)
                    .orElseThrow(() -> new HubEntityNotFoundException("The hub was not found in metadata"));
        } catch (ResolverException e) {
            throw new RuntimeException(e);
        }
    }

    private EntityDescriptor addValidUntilTime(EntityDescriptor entityDescriptor) {
        entityDescriptor.setValidUntil(getValidUntil());
        return entityDescriptor;
    }

    public EntityDescriptor copyEntityDescriptor(EntityDescriptor entityDescriptor) {
        String entityDescriptorString = entityDescriptorElementTransformer.apply(entityDescriptor);
        return elementEntityDescriptorTransformer.apply(entityDescriptorString);
    }


    public DateTime getValidUntil() {
        return DateTime.now().plus(samlProxyConfiguration.getMetadataValidDuration().toMilliseconds());
    }
}
