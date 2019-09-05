package uk.gov.ida.hub.samlsoapproxy.logging;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.eventemitter.EventDetailsKey;
import uk.gov.ida.eventemitter.EventEmitter;
import uk.gov.ida.hub.shared.eventsink.EventSinkHubEvent;
import uk.gov.ida.hub.shared.eventsink.EventSinkProxy;
import uk.gov.ida.exceptions.ApplicationException;

import java.net.URI;
import java.util.Map;

import static uk.gov.ida.common.SessionId.NO_SESSION_CONTEXT_IN_ERROR;
import static uk.gov.ida.eventemitter.EventDetailsKey.downstream_uri;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.EventTypes.ERROR_EVENT;

public class HealthCheckEventLogger {
    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckEventLogger.class);
    private EventSinkProxy eventSinkProxy;
    private EventEmitter eventEmitter;
    private ServiceInfoConfiguration serviceInfo;

    @Inject
    public HealthCheckEventLogger(EventSinkProxy eventSinkProxy,
                                  EventEmitter eventEmitter,
                                  ServiceInfoConfiguration serviceInfo){
        this.eventSinkProxy = eventSinkProxy;
        this.eventEmitter = eventEmitter;
        this.serviceInfo = serviceInfo;
    }

    public void logException(ApplicationException exception, String message) {
        LOG.warn(message, exception);

        if (exception.isAudited() || !exception.requiresAuditing()) {
            return;
        }

        Map<EventDetailsKey, String> details = Map.of(
                downstream_uri, exception.getUri().or(URI.create("uri-not-present")).toASCIIString(),
                EventDetailsKey.message, exception.getMessage());

        EventSinkHubEvent hubEvent = new EventSinkHubEvent(serviceInfo, NO_SESSION_CONTEXT_IN_ERROR, ERROR_EVENT, details);
        eventSinkProxy.logHubEvent(hubEvent);
        eventEmitter.record(hubEvent);
    }
}
