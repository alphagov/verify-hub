package uk.gov.ida.hub.policy.resources;

import com.codahale.metrics.annotation.Timed;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.controllogic.ResponseFromIdpHandler;
import uk.gov.ida.hub.policy.domain.FailureResponseDetails;
import uk.gov.ida.hub.policy.domain.ResponseProcessingDetails;
import uk.gov.ida.hub.policy.domain.SessionId;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * This resource is called from frontend when its waiting for the matching to happen in the
 * background in order to respond back to the user based on the policy state.
 */
@Path(Urls.PolicyUrls.RESPONSE_FROM_IDP_ROOT)
@Produces(MediaType.APPLICATION_JSON)
public class ResponseFromIdpResource {

    private final ResponseFromIdpHandler responseFromIdpHandler;

    @Inject
    public ResponseFromIdpResource(
            ResponseFromIdpHandler responseFromIdpHandler) {

        this.responseFromIdpHandler = responseFromIdpHandler;
    }

    @GET
    @Path(Urls.PolicyUrls.RESPONSE_PROCESSING_DETAILS_PATH)
    @Timed
    public ResponseProcessingDetails getResponseProcessingDetails(@PathParam(Urls.SharedUrls.SESSION_ID_PARAM) SessionId sessionId) {
        return responseFromIdpHandler.getResponseProcessingDetails(sessionId);
    }

    @GET
    @Path(Urls.PolicyUrls.FAILURE_DETAILS_PATH)
    @Timed
    public FailureResponseDetails getErrorResponseFromIdp(@PathParam(Urls.SharedUrls.SESSION_ID_PARAM) SessionId sessionId) {
        return responseFromIdpHandler.getErrorResponseFromIdp(sessionId);
    }
}
