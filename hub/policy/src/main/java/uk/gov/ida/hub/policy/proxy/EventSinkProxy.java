package uk.gov.ida.hub.policy.proxy;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.eventsink.EventSink;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.domain.EventSinkHubEvent;
import uk.gov.ida.jerseyclient.JsonClient;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

@Singleton
public class EventSinkProxy {
    private static final Logger LOG = LoggerFactory.getLogger(EventSinkProxy.class);
    private final JsonClient jsonClient;
    private final URI eventSinkUri;
    private final Environment environment;

    @Inject
    public EventSinkProxy(
            JsonClient jsonClient,
            @EventSink URI eventSinkUri,
            Environment environment) {
        this.jsonClient = jsonClient;
        this.eventSinkUri = eventSinkUri;
        this.environment = environment;
    }

    @Timed
    public void logHubEvent(EventSinkHubEvent eventSinkHubEvent) {
        String path = Urls.HubSupportUrls.HUB_SUPPORT_EVENT_SINK_RESOURCE;
        URI uri = UriBuilder
                .fromUri(eventSinkUri)
                .path(path)
                .build();
        try {
            jsonClient.post(eventSinkHubEvent, uri);
            LOG.info("Sent to Event Sink " + eventSinkHubEvent.getEventType() + " hub event to event-sink on " + uri);

        } catch (ApplicationException e) {

            LOG.error("Failed to send event to event sink.", e);
            LOG.warn("failed event: {}", getEventAsString(eventSinkHubEvent));
        }
    }

    private String getEventAsString(EventSinkHubEvent eventSinkHubEvent) {
        try {
            return environment.getObjectMapper().writeValueAsString(eventSinkHubEvent);
        } catch (JsonProcessingException e) {
            LOG.error("Unable to serialize hub event for logger.", e);
            return "unable to serialize event.";
        }
    }
}
