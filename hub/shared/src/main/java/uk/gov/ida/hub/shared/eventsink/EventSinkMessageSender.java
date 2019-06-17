package uk.gov.ida.hub.shared.eventsink;

import com.google.common.collect.ImmutableMap;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.eventemitter.EventDetailsKey;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class EventSinkMessageSender {

    private EventSinkProxy eventSinkProxy;
    private ServiceInfoConfiguration serviceInfo;

    @Inject
    public EventSinkMessageSender(EventSinkProxy eventSinkProxy, ServiceInfoConfiguration serviceInfo) {
        this.eventSinkProxy = eventSinkProxy;
        this.serviceInfo = serviceInfo;
    }

    public void audit(Exception exception, UUID errorId, SessionId sessionId, EventDetails... additionalDetails) {
        Map<EventDetailsKey, String> details = new HashMap<>(ImmutableMap.of(
                EventDetailsKey.error_id, errorId.toString(),
                EventDetailsKey.message, exception.getMessage())
        );

        for (EventDetails simpleEntry : additionalDetails) {
            details.put(simpleEntry.getKey(), simpleEntry.getValue());
        }

        EventSinkHubEvent event = new EventSinkHubEvent(this.serviceInfo, sessionId, EventSinkHubEventConstants.EventTypes.ERROR_EVENT, details);
        eventSinkProxy.logHubEvent(event);
    }
}
