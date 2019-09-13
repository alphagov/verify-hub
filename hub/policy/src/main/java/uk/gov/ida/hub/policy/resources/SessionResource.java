package uk.gov.ida.hub.policy.resources;

import com.codahale.metrics.annotation.Timed;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.contracts.SamlAuthnResponseContainerDto;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.ResponseAction;
import uk.gov.ida.hub.policy.domain.SamlAuthnRequestContainerDto;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.services.AuthnResponseFromIdpService;
import uk.gov.ida.hub.policy.services.SessionService;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.Optional;

import static uk.gov.ida.hub.policy.Urls.PolicyUrls.IDP_AUTHN_REQUEST_PATH;
import static uk.gov.ida.hub.policy.Urls.PolicyUrls.IDP_AUTHN_RESPONSE_PATH;
import static uk.gov.ida.hub.policy.Urls.PolicyUrls.LOA_FOR_SESSION_PATH;
import static uk.gov.ida.hub.policy.Urls.PolicyUrls.RP_AUTHN_RESPONSE_PATH;
import static uk.gov.ida.hub.policy.Urls.PolicyUrls.RP_ERROR_RESPONSE_PATH;
import static uk.gov.ida.hub.policy.Urls.SharedUrls.SESSION_ID_PARAM;
import static uk.gov.ida.hub.policy.Urls.SharedUrls.SESSION_ID_PARAM_PATH;

@Path(Urls.PolicyUrls.SESSION_RESOURCE_ROOT)
@Produces(MediaType.APPLICATION_JSON)
public class SessionResource {

    private final SessionService sessionService;
    private final AuthnResponseFromIdpService authnResponseFromIdpService;

    @Inject
    public SessionResource(SessionService service,
                           AuthnResponseFromIdpService authnResponseFromIdpService) {
        this.sessionService = service;
        this.authnResponseFromIdpService = authnResponseFromIdpService;
    }

    @GET
    @Path(SESSION_ID_PARAM_PATH)
    public SessionId getSession(@PathParam(SESSION_ID_PARAM) SessionId sessionIdParameter) {
        return sessionService.getSessionIfItExists(sessionIdParameter);
    }

    @GET
    @Path(LOA_FOR_SESSION_PATH)
    public Optional<LevelOfAssurance> getLevelOfAssurance(@PathParam(SESSION_ID_PARAM) SessionId sessionIdParameter) {
        return sessionService.getLevelOfAssurance(sessionIdParameter);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Timed
    public Response createSession(SamlAuthnRequestContainerDto requestDto) {
        SessionId sessionId = sessionService.create(requestDto);
        return Response.created(UriBuilder.fromPath(SESSION_ID_PARAM_PATH).build(sessionId.getSessionId())).entity(sessionId).build();
    }

    @GET
    @Path(IDP_AUTHN_REQUEST_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Timed
    public Response getIdpAuthnRequestFromHub(@PathParam(SESSION_ID_PARAM) SessionId sessionId) {
        return Response.ok().entity(sessionService.getIdpAuthnRequest(sessionId)).build();
    }

    @POST
    @Path(IDP_AUTHN_RESPONSE_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Timed
    public Response receiveAuthnResponseFromIdp(@PathParam(SESSION_ID_PARAM) SessionId sessionId, SamlAuthnResponseContainerDto samlResponseDto) {
        ResponseAction responseAction = authnResponseFromIdpService.receiveAuthnResponseFromIdp(sessionId, samlResponseDto);
        return Response.ok().entity(responseAction).build();
    }

    @GET
    @Path(RP_AUTHN_RESPONSE_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Timed
    public Response getRpAuthnResponse(@PathParam(SESSION_ID_PARAM) SessionId sessionId) {
        return Response.ok(sessionService.getRpAuthnResponse(sessionId)).build();
    }

    @GET
    @Path(RP_ERROR_RESPONSE_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Timed
    public Response getRpErrorResponse(@PathParam(SESSION_ID_PARAM) SessionId sessionId) {
        return Response.ok(sessionService.getRpErrorResponse(sessionId)).build();
    }
}
