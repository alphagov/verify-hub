package uk.gov.ida.hub.policy.resources;

import com.codahale.metrics.annotation.Timed;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.controllogic.AuthnRequestFromTransactionHandler;
import uk.gov.ida.hub.policy.domain.AuthnRequestSignInDetailsDto;
import uk.gov.ida.hub.policy.domain.AuthnRequestSignInProcess;
import uk.gov.ida.hub.policy.domain.IdpSelected;
import uk.gov.ida.hub.policy.domain.SessionId;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static uk.gov.ida.hub.policy.Urls.SharedUrls.SESSION_ID_PARAM;

@Path(Urls.PolicyUrls.AUTHN_REQUEST_FROM_TRANSACTION_ROOT)
@Produces(MediaType.APPLICATION_JSON)
public class AuthnRequestFromTransactionResource {
    private final AuthnRequestFromTransactionHandler authnRequestFromTransactionHandler;

    @Inject
    public AuthnRequestFromTransactionResource(
            AuthnRequestFromTransactionHandler authnRequestFromTransactionHandler) {
        this.authnRequestFromTransactionHandler = authnRequestFromTransactionHandler;
    }

    @POST
    @Path(Urls.PolicyUrls.AUTHN_REQUEST_SELECT_IDP_PATH)
    @Timed
    public Response selectIdentityProvider(
            @PathParam(SESSION_ID_PARAM) SessionId sessionIdParameter, @Valid IdpSelected idpSelected) {

        authnRequestFromTransactionHandler.selectIdpForGivenSessionId(sessionIdParameter, idpSelected);

        return Response.status(Response.Status.CREATED).build();
    }

    @POST
    @Path(Urls.PolicyUrls.AUTHN_REQUEST_TRY_ANOTHER_IDP_PATH)
    @Timed
    public void tryAnotherIdp(@PathParam(SESSION_ID_PARAM) SessionId sessionId) {
        authnRequestFromTransactionHandler.tryAnotherIdp(sessionId);
    }

    @POST
    @Path(Urls.PolicyUrls.AUTHN_REQUEST_RESTART_EIDAS_JOURNEY_PATH)
    @Timed
    public void restartEidasJourney(@PathParam(SESSION_ID_PARAM) SessionId sessionId) {
        authnRequestFromTransactionHandler.restartEidasUnsuccessfulJourney(sessionId);
    }

    @GET
    @Path(Urls.PolicyUrls.AUTHN_REQUEST_SIGN_IN_PROCESS_DETAILS_PATH)
    @Timed
    public AuthnRequestSignInDetailsDto getSignInProcessDto(@PathParam(SESSION_ID_PARAM) SessionId sessionId) {
        AuthnRequestSignInProcess signInProcess = authnRequestFromTransactionHandler.getSignInProcessDto(sessionId);
        return new AuthnRequestSignInDetailsDto(
                signInProcess.getRequestIssuerId(),
                signInProcess.getTransactionSupportsEidas());
    }

    @GET
    @Path(Urls.PolicyUrls.AUTHN_REQUEST_ISSUER_ID_PATH)
    @Timed
    public String getRequestIssuerId(@PathParam(SESSION_ID_PARAM) SessionId sessionId) {
        return authnRequestFromTransactionHandler.getRequestIssuerId(sessionId);
    }
}
