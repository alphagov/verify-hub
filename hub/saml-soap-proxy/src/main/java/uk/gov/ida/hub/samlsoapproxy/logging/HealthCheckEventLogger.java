package uk.gov.ida.hub.samlsoapproxy.logging;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.eventsink.EventDetailsKey;
import uk.gov.ida.eventsink.EventSinkHubEvent;
import uk.gov.ida.eventsink.EventSinkProxy;
import uk.gov.ida.exceptions.ApplicationException;

import java.net.URI;

import static uk.gov.ida.common.SessionId.NO_SESSION_CONTEXT_IN_ERROR;
import static uk.gov.ida.eventsink.EventDetailsKey.downstream_uri;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.EventTypes.ERROR_EVENT;

public class HealthCheckEventLogger {
    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckEventLogger.class);
    private EventSinkProxy eventSinkProxy;
    private ServiceInfoConfiguration serviceInfo;

    @Inject
    public HealthCheckEventLogger(EventSinkProxy eventSinkProxy,
                                  ServiceInfoConfiguration serviceInfo){
        this.eventSinkProxy = eventSinkProxy;
        this.serviceInfo = serviceInfo;
    }

    public void logException(ApplicationException exception, String message) {
        LOG.warn(message, exception);

        if (exception.isAudited() || !exception.requiresAuditing()) {
            return;
        }

        ImmutableMap<EventDetailsKey, String> details = ImmutableMap.of(
                downstream_uri, exception.getUri().or(URI.create("uri-not-present")).toASCIIString(),
                EventDetailsKey.message, exception.getMessage());

        eventSinkProxy.logHubEvent(new EventSinkHubEvent(serviceInfo, NO_SESSION_CONTEXT_IN_ERROR, ERROR_EVENT, details));
    }
}
