package uk.gov.ida.hub.samlengine.resources.translators;

import com.codahale.metrics.annotation.Timed;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.domain.AttributeQueryRequestDto;
import uk.gov.ida.hub.samlengine.services.MatchingServiceRequestGeneratorService;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Produces(MediaType.APPLICATION_JSON)
@Path(Urls.SamlEngineUrls.GENERATE_ATTRIBUTE_QUERY_RESOURCE)
public class MatchingServiceRequestGeneratorResource {

    private final MatchingServiceRequestGeneratorService service;

    @Inject
    public MatchingServiceRequestGeneratorResource(MatchingServiceRequestGeneratorService service) {
        this.service = service;
    }

    @POST
    @Timed
    public Response generateAttributeQuery(final AttributeQueryRequestDto dto) throws IOException {
        return Response.ok().entity(service.generate(dto)).build();
    }

}
