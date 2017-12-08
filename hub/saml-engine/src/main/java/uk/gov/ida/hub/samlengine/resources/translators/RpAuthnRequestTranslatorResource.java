package uk.gov.ida.hub.samlengine.resources.translators;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.SamlRequestWithAuthnRequestInformationDto;
import uk.gov.ida.hub.samlengine.contracts.TranslatedAuthnRequestDto;
import uk.gov.ida.hub.samlengine.services.RpAuthnRequestTranslatorService;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(Urls.SamlEngineUrls.TRANSLATE_RP_AUTHN_REQUEST_RESOURCE)
public class RpAuthnRequestTranslatorResource {

    private final RpAuthnRequestTranslatorService rpAuthnRequestTranslatorService;

    @Inject
    public RpAuthnRequestTranslatorResource(RpAuthnRequestTranslatorService rpAuthnRequestTranslatorService) {
        this.rpAuthnRequestTranslatorService = rpAuthnRequestTranslatorService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    public Response translate(SamlRequestWithAuthnRequestInformationDto samlRequestWithAuthnRequestInformationDto) throws JsonProcessingException {
        TranslatedAuthnRequestDto translated = rpAuthnRequestTranslatorService.translate(samlRequestWithAuthnRequestInformationDto);

        return Response.ok().entity(translated).type(MediaType.APPLICATION_JSON_TYPE).build();
    }
}
