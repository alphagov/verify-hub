package uk.gov.ida.integrationtest.hub.policy.apprule.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import httpstub.HttpStubRule;
import uk.gov.ida.hub.policy.Urls;

import javax.ws.rs.core.Response;

public class EventSinkStubRule extends HttpStubRule {
    public void setupStubForLogging() throws JsonProcessingException {
        register(Urls.HubSupportUrls.HUB_SUPPORT_EVENT_SINK_RESOURCE, Response.Status.OK.getStatusCode());
    }

}
