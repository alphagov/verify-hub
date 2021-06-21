package uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support;

import httpstub.HttpStubRule;
import uk.gov.ida.hub.samlsoapproxy.Urls;

import javax.ws.rs.core.Response;

public class EventSinkStubRule extends HttpStubRule {
    public void setupStubForLogging() {
        register(Urls.HubSupportUrls.HUB_SUPPORT_EVENT_SINK_RESOURCE, Response.Status.OK.getStatusCode());
    }

}
