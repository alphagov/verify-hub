package uk.gov.ida.hub.samlengine.resources.translators;

import com.codahale.metrics.annotation.Timed;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.saml.core.domain.AuthnResponseFromCountryContainerDto;
import uk.gov.ida.hub.samlengine.services.RpAuthnResponseGeneratorService;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(Urls.SamlEngineUrls.GENERATE_RP_AUTHN_RESPONSE_WRAPPING_COUNTRY_RESPONSE_RESOURCE)
public class RpAuthnResponseWrappingCountryResponseGeneratorResource {

    private RpAuthnResponseGeneratorService service;

    @Inject
    public RpAuthnResponseWrappingCountryResponseGeneratorResource(RpAuthnResponseGeneratorService service) {
        this.service = service;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    public Response generate(AuthnResponseFromCountryContainerDto responseFromHub) {
        return Response.ok().entity(service.generate(responseFromHub)).type(MediaType.APPLICATION_JSON_TYPE).build();
    }
}
