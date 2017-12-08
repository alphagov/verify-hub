package uk.gov.ida.hub.samlengine.resources.translators;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.ResponseFromHubDto;
import uk.gov.ida.hub.samlengine.services.RpAuthnResponseGeneratorService;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(Urls.SamlEngineUrls.GENERATE_RP_AUTHN_RESPONSE_RESOURCE)
public class RpAuthnResponseGeneratorResource {

    private RpAuthnResponseGeneratorService service;

    @Inject
    public RpAuthnResponseGeneratorResource(RpAuthnResponseGeneratorService service) {
        this.service = service;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    public Response generate(ResponseFromHubDto responseFromHub) throws JsonProcessingException {
        return Response.ok().entity(service.generate(responseFromHub)).type(MediaType.APPLICATION_JSON_TYPE).build();
    }
}
