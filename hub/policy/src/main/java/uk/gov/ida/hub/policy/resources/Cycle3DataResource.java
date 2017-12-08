package uk.gov.ida.hub.policy.resources;

import com.codahale.metrics.annotation.Timed;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.domain.Cycle3AttributeRequestData;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.Cycle3UserInput;
import uk.gov.ida.hub.policy.services.Cycle3Service;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path(Urls.PolicyUrls.CYCLE_3_REQUEST_ROOT)
@Produces(MediaType.APPLICATION_JSON)
public class Cycle3DataResource {

    private Cycle3Service cycle3service;

    @Inject
    public Cycle3DataResource(Cycle3Service cycle3service) {
        this.cycle3service = cycle3service;
    }

    @GET
    @Timed
    public Cycle3AttributeRequestData getCycle3AttributeRequestData(@PathParam(Urls.SharedUrls.SESSION_ID_PARAM) SessionId sessionId) {
        return cycle3service.getCycle3AttributeRequestData(sessionId);
    }

    @POST
    @Path(Urls.PolicyUrls.CYCLE_3_SUBMIT_PATH)
    @Timed
    public void submitCycle3Data(@PathParam(Urls.SharedUrls.SESSION_ID_PARAM) SessionId sessionId, Cycle3UserInput cycle3UserInput) {
        cycle3service.sendCycle3MatchingRequest(sessionId, cycle3UserInput);
    }

    @POST
    @Path(Urls.PolicyUrls.CYCLE_3_CANCEL_PATH)
    @Timed
    public void cancelCycle3DataInput(@PathParam(Urls.SharedUrls.SESSION_ID_PARAM) SessionId sessionId) {
        cycle3service.cancelCycle3DataInput(sessionId);
    }


}
