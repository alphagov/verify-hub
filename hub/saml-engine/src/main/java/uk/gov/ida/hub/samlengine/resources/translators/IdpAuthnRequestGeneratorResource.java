package uk.gov.ida.hub.samlengine.resources.translators;

import com.codahale.metrics.annotation.Timed;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.IdaAuthnRequestFromHubDto;
import uk.gov.ida.hub.samlengine.domain.SamlRequestDto;
import uk.gov.ida.hub.samlengine.services.IdpAuthnRequestGeneratorService;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(Urls.SamlEngineUrls.GENERATE_IDP_AUTHN_REQUEST_RESOURCE)
public class IdpAuthnRequestGeneratorResource {

    private final IdpAuthnRequestGeneratorService idpAuthnRequestGeneratorService;

    @Inject
    public IdpAuthnRequestGeneratorResource(IdpAuthnRequestGeneratorService idpAuthnRequestGeneratorService) {
        this.idpAuthnRequestGeneratorService = idpAuthnRequestGeneratorService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    public Response generate(IdaAuthnRequestFromHubDto idaAuthnRequestFromHubDto) {
        SamlRequestDto samlRequestDto = idpAuthnRequestGeneratorService.generateSaml(idaAuthnRequestFromHubDto);
        return Response.ok().entity(samlRequestDto).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

}
