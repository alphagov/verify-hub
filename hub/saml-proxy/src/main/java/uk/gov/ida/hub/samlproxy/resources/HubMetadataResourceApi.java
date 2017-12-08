package uk.gov.ida.hub.samlproxy.resources;

import com.codahale.metrics.annotation.Timed;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import uk.gov.ida.hub.samlproxy.Urls;
import uk.gov.ida.hub.samlproxy.handlers.HubAsIdpMetadataHandler;
import uk.gov.ida.hub.samlproxy.handlers.HubAsSpMetadataHandler;
import uk.gov.ida.saml.metadata.domain.HubIdentityProviderMetadataDto;
import uk.gov.ida.saml.serializers.XmlObjectToElementTransformer;
import uk.gov.ida.hub.samlproxy.domain.SamlDto;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.function.Function;

@Path(Urls.SamlProxyUrls.METADATA_API_ROOT)
@Produces("application/json")
public class HubMetadataResourceApi {
    private final Function<HubIdentityProviderMetadataDto, Element> hubMetadataAsIdpTransformer;
    private final XmlObjectToElementTransformer<EntityDescriptor> entityDescriptorToElementTransformer;
    private final HubAsIdpMetadataHandler hubAsIdpMetadataHandler;
    private final HubAsSpMetadataHandler  hubAsSpMetadataHandler;

    @Inject
    public HubMetadataResourceApi(
            Function<HubIdentityProviderMetadataDto, Element> hubMetadataAsIdpTransformer,
            XmlObjectToElementTransformer<EntityDescriptor> entityDescriptorToElementTransformer,
            HubAsIdpMetadataHandler hubAsIdpMetadataHandler,
            HubAsSpMetadataHandler  hubAsSpMetadataHandler
    ) {
        this.hubMetadataAsIdpTransformer = hubMetadataAsIdpTransformer;
        this.entityDescriptorToElementTransformer = entityDescriptorToElementTransformer;
        this.hubAsIdpMetadataHandler = hubAsIdpMetadataHandler;
        this.hubAsSpMetadataHandler  = hubAsSpMetadataHandler;
    }

    @GET
    @Path(Urls.SamlProxyUrls.SP_METADATA_PATH)
    @Timed
    public SamlDto getSpMetadata() {
        return new SamlDto(getSpMetadataDocument());
    }

    @GET
    @Path(Urls.SamlProxyUrls.IDP_METADATA_PATH)
    @Timed
    public SamlDto getIdpMetadata() {
        return new SamlDto(getIdpMetadataDocument());
    }

    protected Document getSpMetadataDocument() {
        return entityDescriptorToElementTransformer.apply(
            hubAsSpMetadataHandler.getMetadataAsAServiceProvider()
        ).getOwnerDocument();
    }

    protected Document getIdpMetadataDocument() {
        return hubMetadataAsIdpTransformer.apply(
            hubAsIdpMetadataHandler.getMetadataAsAnIdentityProvider()
        ).getOwnerDocument();
    }

}
