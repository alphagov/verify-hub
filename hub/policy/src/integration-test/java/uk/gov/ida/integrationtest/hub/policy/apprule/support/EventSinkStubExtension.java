package uk.gov.ida.integrationtest.hub.policy.apprule.support;

import httpstub.HttpStubExtension;
import uk.gov.ida.hub.policy.Urls;

import javax.ws.rs.core.Response;

public class EventSinkStubExtension extends HttpStubExtension {
    public void setupStubForLogging() {
        register(Urls.HubSupportUrls.HUB_SUPPORT_EVENT_SINK_RESOURCE, Response.Status.OK.getStatusCode());
    }
}
