package uk.gov.ida.hub.shared.eventsink;

import uk.gov.ida.eventemitter.Event;

public interface EventSinkProxy {

    void logHubEvent(Event eventSinkHubEvent);
}
