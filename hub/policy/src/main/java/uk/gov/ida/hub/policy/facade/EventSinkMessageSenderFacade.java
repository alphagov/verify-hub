package uk.gov.ida.hub.policy.facade;

import com.google.common.collect.ImmutableMap;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.eventsink.EventDetails;
import uk.gov.ida.eventsink.EventDetailsKey;
import uk.gov.ida.eventsink.EventSinkHubEventConstants;
import uk.gov.ida.eventsink.EventSinkProxy;
import uk.gov.ida.hub.policy.domain.EventSinkHubEvent;
import uk.gov.ida.hub.policy.domain.SessionId;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EventSinkMessageSenderFacade {

    private EventSinkProxy eventSinkProxy;
    private ServiceInfoConfiguration serviceInfo;

    @Inject
    public EventSinkMessageSenderFacade(EventSinkProxy eventSinkProxy, ServiceInfoConfiguration serviceInfo) {
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
