package uk.gov.ida.hub.samlengine.resources.translators;

import com.codahale.metrics.annotation.Timed;
import javax.inject.Inject;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.MatchingServiceHealthCheckerResponseDto;
import uk.gov.ida.hub.samlengine.domain.SamlMessageDto;
import uk.gov.ida.hub.samlengine.services.MatchingServiceHealthcheckResponseTranslatorService;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(Urls.SamlEngineUrls.TRANSLATE_MSA_HEALTHCHECK_ATTRIBUTE_QUERY_RESPONSE_RESOURCE)
public class MatchingServiceHealthcheckResponseTranslatorResource {

    private final MatchingServiceHealthcheckResponseTranslatorService matchingServiceHealthcheckResponseTranslatorService;

    @Inject
    public MatchingServiceHealthcheckResponseTranslatorResource(MatchingServiceHealthcheckResponseTranslatorService matchingServiceHealthcheckResponseTranslatorService) {
        this.matchingServiceHealthcheckResponseTranslatorService = matchingServiceHealthcheckResponseTranslatorService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    public Response translate(SamlMessageDto samlMessageDto) {
        MatchingServiceHealthCheckerResponseDto translated = matchingServiceHealthcheckResponseTranslatorService.translate(samlMessageDto);

        return Response.ok().entity(translated).type(MediaType.APPLICATION_JSON_TYPE).build();
    }
}
