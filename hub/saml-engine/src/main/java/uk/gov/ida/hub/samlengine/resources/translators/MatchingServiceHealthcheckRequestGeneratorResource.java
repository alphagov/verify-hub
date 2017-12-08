package uk.gov.ida.hub.samlengine.resources.translators;


import com.codahale.metrics.annotation.Timed;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.MatchingServiceHealthCheckerRequestDto;
import uk.gov.ida.hub.samlengine.services.MatchingServiceHealthcheckRequestGeneratorService;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Produces(MediaType.APPLICATION_JSON)
@Path(Urls.SamlEngineUrls.GENERATE_MSA_HEALTHCHECK_ATTRIBUTE_QUERY_RESOURCE)
public class MatchingServiceHealthcheckRequestGeneratorResource {

    private final MatchingServiceHealthcheckRequestGeneratorService service;

    @Inject
    public MatchingServiceHealthcheckRequestGeneratorResource(MatchingServiceHealthcheckRequestGeneratorService service) {
        this.service = service;
    }

    @POST
    @Timed
    public Response generateAttributeQuery(final MatchingServiceHealthCheckerRequestDto dto) throws IOException {
        return Response.ok().entity(service.generate(dto)).build();
    }

}