package uk.gov.ida.hub.samlengine.resources.translators;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.RequestForErrorResponseFromHubDto;
import uk.gov.ida.hub.samlengine.domain.SamlMessageDto;
import uk.gov.ida.hub.samlengine.services.RpErrorResponseGeneratorService;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(Urls.SamlEngineUrls.GENERATE_RP_ERROR_RESPONSE_RESOURCE)
public class RpErrorResponseGeneratorResource {

    private RpErrorResponseGeneratorService rpErrorResponseGeneratorService;

    @Inject
    public RpErrorResponseGeneratorResource(RpErrorResponseGeneratorService rpErrorResponseGeneratorService) {
        this.rpErrorResponseGeneratorService = rpErrorResponseGeneratorService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    public Response generate(RequestForErrorResponseFromHubDto requestForErrorResponseFromHubDto) throws JsonProcessingException {
        SamlMessageDto response = rpErrorResponseGeneratorService.generate(requestForErrorResponseFromHubDto);
        return Response.ok().entity(response).type(MediaType.APPLICATION_JSON_TYPE).build();
    }
}
