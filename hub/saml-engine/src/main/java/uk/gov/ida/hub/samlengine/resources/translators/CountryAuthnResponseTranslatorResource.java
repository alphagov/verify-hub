package uk.gov.ida.hub.samlengine.resources.translators;

import com.codahale.metrics.annotation.Timed;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.SamlAuthnResponseTranslatorDto;
import uk.gov.ida.hub.samlengine.domain.InboundResponseFromCountry;
import uk.gov.ida.hub.samlengine.services.CountryAuthnResponseTranslatorService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(Urls.SamlEngineUrls.TRANSLATE_COUNTRY_AUTHN_RESPONSE_RESOURCE)
public class CountryAuthnResponseTranslatorResource {

    private final CountryAuthnResponseTranslatorService countryAuthnResponseTranslatorService;

    @Inject
    public CountryAuthnResponseTranslatorResource(CountryAuthnResponseTranslatorService countryAuthnResponseTranslatorService) {
        this.countryAuthnResponseTranslatorService = countryAuthnResponseTranslatorService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    public Response translate(@Valid SamlAuthnResponseTranslatorDto samlResponseDto) {
        InboundResponseFromCountry translated = countryAuthnResponseTranslatorService.translate(samlResponseDto);
        return Response.ok().entity(translated).type(MediaType.APPLICATION_JSON_TYPE).build();
    }
}
