package uk.gov.ida.hub.policy.domain;

import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.eventemitter.Event;
import uk.gov.ida.eventemitter.EventDetailsKey;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class EventSinkHubEvent implements Event {

    private final UUID eventId;
    private final DateTime timestamp = DateTime.now();
    private final String originatingService;
    private final String sessionId;
    private final String eventType;
    private final EnumMap<EventDetailsKey, String> details;

    public EventSinkHubEvent(ServiceInfoConfiguration serviceInfo, SessionId sessionId, String eventType, Map<EventDetailsKey, String> details) {
        this.eventId = UUID.randomUUID();
        this.originatingService = serviceInfo.getName();
        this.sessionId = sessionId.getSessionId();
        this.eventType = eventType;
        this.details = Maps.newEnumMap(details);
    }

    public UUID getEventId() {
        return eventId;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public String getOriginatingService() {
        return originatingService;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getEventType() {
        return eventType;
    }

    public EnumMap<EventDetailsKey, String> getDetails() {
        return details;
    }
}
