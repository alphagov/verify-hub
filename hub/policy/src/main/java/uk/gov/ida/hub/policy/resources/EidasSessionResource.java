package uk.gov.ida.hub.policy.resources;

import com.codahale.metrics.annotation.Timed;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.contracts.SamlAuthnResponseContainerDto;
import uk.gov.ida.hub.policy.domain.ResponseAction;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.services.AuthnResponseFromCountryService;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static uk.gov.ida.hub.policy.Urls.PolicyUrls.COUNTRY_AUTHN_RESPONSE_PATH;
import static uk.gov.ida.hub.policy.Urls.SharedUrls.SESSION_ID_PARAM;

@Path(Urls.PolicyUrls.EIDAS_SESSION_RESOURCE_ROOT)
@Produces(MediaType.APPLICATION_JSON)
public class EidasSessionResource {

    private final AuthnResponseFromCountryService authnResponseFromCountryService;

    @Inject
    public EidasSessionResource(AuthnResponseFromCountryService authnResponseFromCountryService) {
        this.authnResponseFromCountryService = authnResponseFromCountryService;
    }

    @POST
    @Path(COUNTRY_AUTHN_RESPONSE_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Timed
    public Response receiveAuthnResponseFromCountry(@PathParam(SESSION_ID_PARAM) SessionId sessionId, SamlAuthnResponseContainerDto samlResponseDto) {
        ResponseAction responseAction = authnResponseFromCountryService.receiveAuthnResponseFromCountry(sessionId, samlResponseDto);
        return Response.ok().entity(responseAction).build();
    }
}
