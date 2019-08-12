package uk.gov.ida.hub.samlengine.resources.translators;

import com.codahale.metrics.annotation.Timed;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.domain.EidasAttributeQueryRequestDto;
import uk.gov.ida.hub.samlengine.services.CountryMatchingServiceRequestGeneratorService;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Produces(MediaType.APPLICATION_JSON)
@Path(Urls.SamlEngineUrls.GENERATE_COUNTRY_ATTRIBUTE_QUERY_RESOURCE)
public class CountryMatchingServiceRequestGeneratorResource {

    private final CountryMatchingServiceRequestGeneratorService service;

    @Inject
    public CountryMatchingServiceRequestGeneratorResource(CountryMatchingServiceRequestGeneratorService service) {
        this.service = service;
    }

    @POST
    @Timed
    public Response generateAttributeQuery(final EidasAttributeQueryRequestDto dto) {
        return Response.ok().entity(service.generate(dto)).build();
    }
}
