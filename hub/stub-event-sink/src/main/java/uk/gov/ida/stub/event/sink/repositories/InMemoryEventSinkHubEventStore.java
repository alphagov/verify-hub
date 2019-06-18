package uk.gov.ida.stub.event.sink.repositories;

import uk.gov.ida.hub.shared.eventsink.EventSinkHubEvent;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryEventSinkHubEventStore {
    private Map<String, EventSinkHubEvent> exceptionMap = new ConcurrentHashMap<>();

    @Inject
    public InMemoryEventSinkHubEventStore() {
    }

    public void add(EventSinkHubEvent eventSinkHubEvent) {
        exceptionMap.put(eventSinkHubEvent.getEventId().toString(), eventSinkHubEvent);
    }

    public EventSinkHubEvent getEventById(String id) {
        return exceptionMap.get(id);
    }

    public List<EventSinkHubEvent> getAllEvents() {
        List<EventSinkHubEvent> list = new ArrayList<>();
        for (String s : exceptionMap.keySet()) {
            list.add(exceptionMap.get(s));
        }
        return list;
    }

    public void deleteAllEvents() {
        exceptionMap.clear();
    }
}
