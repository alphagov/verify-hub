package uk.gov.ida.hub.samlengine.resources.translators;

import com.codahale.metrics.annotation.Timed;
import io.prometheus.client.Counter;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.IdaAuthnRequestFromHubDto;
import uk.gov.ida.hub.samlengine.domain.SamlRequestDto;
import uk.gov.ida.hub.samlengine.services.CountryAuthnRequestGeneratorService;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(Urls.SamlEngineUrls.GENERATE_COUNTRY_AUTHN_REQUEST_RESOURCE)
public class CountryAuthnRequestGeneratorResource {

    private final CountryAuthnRequestGeneratorService countryAuthnRequestGeneratorService;

    private static final Counter authnRequestsToCountries = Counter.build(
            "verify_eidas_connector_requests_total",
            "Total number of EIDAS Connector Requests")
            .labelNames("entity_id")
            .register();

    @Inject
    public CountryAuthnRequestGeneratorResource(CountryAuthnRequestGeneratorService countryAuthnRequestGeneratorService) {
        this.countryAuthnRequestGeneratorService = countryAuthnRequestGeneratorService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    public Response generate(IdaAuthnRequestFromHubDto idaAuthnRequestFromHubDto) {
        SamlRequestDto samlRequestDto = countryAuthnRequestGeneratorService.generateSaml(idaAuthnRequestFromHubDto);
        if (samlRequestDto != null) {
            authnRequestsToCountries.labels(idaAuthnRequestFromHubDto.getIdpEntityId()).inc();
            return Response.ok().entity(samlRequestDto).type(MediaType.APPLICATION_JSON_TYPE).build();
        }
        return Response.status(Response.Status.BAD_REQUEST).build();
    }
}
